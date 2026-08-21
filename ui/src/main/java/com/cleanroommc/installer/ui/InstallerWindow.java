package com.cleanroommc.installer.ui;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.java.JavaResolver;
import com.cleanroommc.installer.java.JavaSpec;
import com.cleanroommc.installer.util.slf4j.LogBridge;
import com.cleanroommc.installer.net.Downloader;
import com.cleanroommc.installer.platform.DetectedLauncher;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.platform.InstallLocations;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.source.Sources;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.InstallResult;
import com.cleanroommc.installer.target.InstallTarget;
import com.cleanroommc.installer.target.InstallTargets;
import com.cleanroommc.installer.target.client.ClientTarget;
import com.cleanroommc.installer.target.mmc.MmcInstance;
import com.cleanroommc.installer.target.mmc.MmcTarget;
import com.cleanroommc.installer.target.server.ServerTarget;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.version.RemoteVersion;
import com.cleanroommc.installer.version.VersionIndex;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The graphical installer. Inspired by {@code CleanroomRelauncher}.
 */
public final class InstallerWindow {

    public static int launch(Environment environment, InstallRequest seed) {
        InstallerWindow window = new InstallerWindow(environment == null ? Environment.current() : environment, seed);
        SwingProgress.run(window::build);
        // Both of these block on purpose: the window must not be usable while an answer it depends
        // on is still unknown, or the user installs without ever seeing the provisioning option, or
        // picks a version out of a list that is still empty.
        window.loadVersions();
        window.detectJava();
        CleanroomUI.showAndWait(window.frame);
        return window.exitCode.get();
    }

    private final Environment environment;
    private final InstallRequest seed;
    private final AtomicInteger exitCode = new AtomicInteger(ExitCode.SUCCESS.code());
    /** The version this jar carries, or null when this is the generic installer. */
    private final String pinnedVersion;
    /** The version the command line asked for, or null. */
    private final String seedVersion;

    private JFrame frame;
    private JTextField directoryField;
    private JCheckBox fullDownload;
    private JCheckBox provisionJava;
    private JCheckBox replaceJavaPath;
    private JCheckBox installScripts;
    private JTextField instanceNameField;
    private JPanel fullDownloadRow;
    private JPanel instanceNameRow;
    private JPanel replaceJavaPathRow;
    private JPanel provisionJavaRow;
    private JPanel installScriptsRow;
    private Rectangle screen;
    private JLabel detected;
    private JLabel modeDescription;
    private JComboBox<RemoteVersion> versionBox;
    private JLabel versionStatus;
    private String selectedTargetId;
    /** The version id to install, or null to let the core pick the newest. */
    private String selectedVersion;

    private InstallerWindow(Environment environment, InstallRequest seed) {
        this.environment = environment;
        this.seed = seed;
        this.selectedTargetId = seed != null && seed.targetId() != null ? seed.targetId() : ClientTarget.ID;
        this.seedVersion = seed == null ? null : seed.version();
        this.pinnedVersion = Sources.pinnedVersion();
        this.selectedVersion = fixedVersion();
    }

    /**
     * The version this run is locked to, or null when the user gets to choose.
     * <p>
     * The command line wins over the jar: a pinned jar asked for another version downloads it.
     */
    private String fixedVersion() {
        return this.seedVersion != null ? this.seedVersion : this.pinnedVersion;
    }

    private void build() {
        CleanroomUI.install();
        this.screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        Rectangle screen = this.screen;
        float scale = CleanroomUI.uiScaleFor(screen);
        CleanroomUI.setUiScale(scale);

        Image icon = Toolkit.getDefaultToolkit().getImage(InstallerWindow.class.getResource("/cleanroom.png"));
        this.frame = new JFrame(InstallerMeta.NAME);
        this.frame.setIconImage(icon);
        this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.frame.setResizable(false);
        this.frame.setLayout(new BorderLayout());
        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel();
            }

            @Override
            public void windowOpened(WindowEvent event) {
                JButton defaultButton = InstallerWindow.this.frame.getRootPane().getDefaultButton();
                if (defaultButton != null) {
                    defaultButton.requestFocusInWindow();
                }
            }
        });

        JPanel container = new JPanel(new BorderLayout());
        CleanroomUI.backgroundPanel(container);

        JPanel column = CleanroomUI.scrollableColumn();
        column.setBorder(new EmptyBorder(18, 24, 24, 24));
        column.add(CleanroomUI.centeredHeader(icon, "Cleanroom Installer", "Choose what to install and where to."));
        column.add(modeCard());
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(versionCard());
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(directoryCard());
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(optionsCard());

        container.add(CleanroomUI.themeToolbar(), BorderLayout.NORTH);
        container.add(CleanroomUI.scrollPane(column), BorderLayout.CENTER);
        container.add(buttons(), BorderLayout.SOUTH);

        this.frame.add(container);
        this.frame.revalidate();
        CleanroomUI.onEscape(this.frame.getRootPane(), this::cancel);

        CleanroomUI.scaleComponent(this.frame, scale);
        CleanroomUI.styleTree(this.frame);
        this.frame.pack();
        CleanroomUI.sizeAndGuard(this.frame, CleanroomUI.dialogSize(screen), this.frame.getPreferredSize());
        this.frame.setLocationRelativeTo(null);
    }

    /**
     * Scans for an installed Java and, only when none is found, reveals the provisioning option.
     * Runs before the window is shown, so what the user sees is already final.
     */
    private void detectJava() {
        JavaSpec spec = this.seed == null ? JavaSpec.defaults() : this.seed.java();
        if (spec.path() != null && !spec.path().isEmpty()) {
            return;
        }
        ProgressWindow progress = progressWindow();
        progress.updateStatus("Looking for " + spec.requirement() + "…");
        progress.show();
        boolean missing;
        try (Log log = Log.console()) {
            missing = !new JavaResolver(this.environment, log).hasLocalJava(spec, new SwingProgress(progress));
        } catch (RuntimeException e) {
            // A failed scan is not proof of anything, so let the user decide.
            missing = true;
        } finally {
            progress.close();
        }
        boolean show = missing;
        SwingProgress.run(() -> revealProvisionJava(show));
    }

    private static ProgressWindow progressWindow() {
        ProgressWindow[] holder = new ProgressWindow[1];
        SwingProgress.run(() -> holder[0] = new ProgressWindow());
        return holder[0];
    }

    private void revealProvisionJava(boolean show) {
        if (!show || this.provisionJavaRow.isVisible()) {
            return;
        }
        this.provisionJavaRow.setVisible(true);
        repack();
    }

    private void repack() {
        this.frame.revalidate();
        this.frame.pack();
        CleanroomUI.sizeAndGuard(this.frame, CleanroomUI.dialogSize(this.screen), this.frame.getPreferredSize());
    }

    /**
     * Fills the version picker, asking the repository every single time.
     * <p>
     * Deliberately not served from the on-disk list: the window is opened once per install, and a
     * user who came here because a release was announced an hour ago must not be shown a list that
     * predates it. The cache stays for the offline fallback below.
     */
    private void loadVersions() {
        if (fixedVersion() != null) {
            return;
        }
        ProgressWindow progress = progressWindow();
        progress.updateStatus("Fetching the Cleanroom version list\u2026");
        progress.show();
        List<RemoteVersion> versions = new ArrayList<>();
        String failure = null;
        try (Log log = Log.console()) {
            Downloader downloader = new Downloader(log, this.seed != null && this.seed.offline());
            VersionIndex index = new VersionIndex(downloader, log, this.environment.installerCache());
            versions.addAll(index.all(false));
        } catch (InstallException e) {
            failure = e.getMessage();
        } catch (RuntimeException e) {
            failure = String.valueOf(e);
        } finally {
            progress.close();
        }
        String message = failure;
        SwingProgress.run(() -> applyVersions(versions, message));
    }

    private void applyVersions(List<RemoteVersion> versions, String failure) {
        if (versions.isEmpty()) {
            this.versionStatus.setText(failure == null
                    ? "No releases were found. The newest release will be installed."
                    : "Unable to list versions (" + failure + "). The newest release will be installed.");
            repack();
            return;
        }
        DefaultComboBoxModel<RemoteVersion> model = new DefaultComboBoxModel<>();
        for (RemoteVersion version : versions) {
            model.addElement(version);
        }
        this.versionBox.setModel(model);
        this.versionBox.setSelectedIndex(0);
        this.versionBox.setEnabled(true);
        this.selectedVersion = versions.get(0).id();
        // styleTree pinned a width to fit the placeholder; the real ids are longer than that.
        int height = this.versionBox.getPreferredSize().height;
        this.versionBox.setPreferredSize(null);
        this.versionBox.setPreferredSize(new Dimension(this.versionBox.getPreferredSize().width, height));
        this.versionStatus.setText("Newest first, straight from " + InstallerMeta.CLEANROOM_REPO + ".");
        repack();
    }

    private JPanel modeCard() {
        JPanel picker = new JPanel(new BorderLayout(5, 0));
        picker.setOpaque(false);

        JPanel select = new JPanel();
        select.setOpaque(false);
        select.setLayout(new BoxLayout(select, BoxLayout.Y_AXIS));
        picker.add(select);

        JPanel dropdown = new JPanel(new BorderLayout(5, 5));
        dropdown.setOpaque(false);
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(dropdown);

        JComboBox<InstallTarget> box = new JComboBox<>();
        DefaultComboBoxModel<InstallTarget> model = new DefaultComboBoxModel<>();
        InstallTarget initial = null;
        for (InstallTarget target : InstallTargets.all()) {
            model.addElement(target);
            if (target.id().equals(this.selectedTargetId)) {
                initial = target;
            }
        }
        box.setModel(model);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof InstallTarget) {
                    setText("Install " + ((InstallTarget) value).displayName());
                }
                return this;
            }
        });
        box.setSelectedItem(initial);
        box.setMaximumRowCount(5);
        box.addActionListener(event -> {
            InstallTarget target = (InstallTarget) box.getSelectedItem();
            if (target != null) {
                this.selectedTargetId = target.id();
                onModeChanged(target);
            }
        });
        dropdown.add(box, BorderLayout.CENTER);

        this.modeDescription = CleanroomUI.statusLabel(initial == null ? "" : initial.description());
        this.modeDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(this.modeDescription);

        return CleanroomUI.card("Installation Mode", "Install Cleanroom in different ways", picker);
    }

    /**
     * The version picker, or — when this run is locked to one version — a plain statement of which
     * version that is. A user who downloaded a version-specific installer still has to be able to
     * see what they are about to install.
     */
    private JPanel versionCard() {
        JPanel picker = new JPanel(new BorderLayout(5, 0));
        picker.setOpaque(false);

        JPanel select = new JPanel();
        select.setOpaque(false);
        select.setLayout(new BoxLayout(select, BoxLayout.Y_AXIS));
        picker.add(select);

        String fixed = fixedVersion();
        if (fixed != null) {
            JLabel version = new JLabel("Cleanroom " + fixed);
            version.setAlignmentX(Component.LEFT_ALIGNMENT);
            select.add(version);
            this.versionStatus = CleanroomUI.wrappingStatusLabel(fixed.equals(this.seedVersion)
                    ? "Asked for on the command line. Drop --version to choose a version here."
                    : "This installer only installs this version.");
            this.versionStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
            select.add(this.versionStatus);
            return CleanroomUI.card("Cleanroom Version", "What this installer will install", picker);
        }

        JPanel dropdown = new JPanel(new BorderLayout(5, 5));
        dropdown.setOpaque(false);
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(dropdown);

        this.versionBox = new JComboBox<>();
        this.versionBox.setModel(new DefaultComboBoxModel<>());
        this.versionBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RemoteVersion) {
                    String id = ((RemoteVersion) value).id();
                    setText(index == 0 || id.equals(firstVersionId()) ? id + " (latest)" : id);
                }
                return this;
            }
        });
        // Nothing to choose from until the fetch below returns.
        this.versionBox.addItem(new RemoteVersion("Loading\u2026", ""));
        this.versionBox.setEnabled(false);
        this.versionBox.setMaximumRowCount(5);
        this.versionBox.addActionListener(event -> {
            RemoteVersion version = (RemoteVersion) this.versionBox.getSelectedItem();
            this.selectedVersion = version == null ? null : version.id();
        });
        dropdown.add(this.versionBox, BorderLayout.CENTER);

        this.versionStatus = CleanroomUI.wrappingStatusLabel("Looking up the available releases\u2026");
        this.versionStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(this.versionStatus);

        return CleanroomUI.card("Cleanroom Version", "Which release to install", picker);
    }

    /** The newest id in the picker, so the renderer can label it whichever row it is drawn for. */
    private String firstVersionId() {
        RemoteVersion first = this.versionBox.getItemCount() == 0 ? null : this.versionBox.getItemAt(0);
        return first == null ? "" : first.id();
    }

    private JPanel directoryCard() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JPanel northPanel = new JPanel(new BorderLayout(5, 0));
        northPanel.setOpaque(false);
        row.add(northPanel, BorderLayout.NORTH);

        this.directoryField = new JTextField(defaultDirectory().toString());
        CleanroomUI.installTextFieldFocus(this.directoryField);
        this.directoryField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                directoryChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                directoryChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                directoryChanged();
            }
        });
        row.add(this.directoryField, BorderLayout.CENTER);

        JButton browse = new JButton("Browse…");
        CleanroomUI.compact(browse);
        CleanroomUI.tooltip(browse, "Choose the folder to install into");
        browse.addActionListener(event -> chooseDirectory());
        row.add(browse, BorderLayout.EAST);

        this.detected = CleanroomUI.wrappingStatusLabel(detectionText());
        row.add(this.detected, BorderLayout.SOUTH);

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(row, BorderLayout.NORTH);
        return CleanroomUI.card("Where", "Folder for the installation", column);
    }

    private JPanel optionsCard() {
        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));

        this.fullDownload = new JCheckBox("Immediately Download Libraries");
        this.fullDownload.setOpaque(false);
        this.fullDownloadRow = CleanroomUI.optionRow(this.fullDownload, "Requires internet now, but not later when running the instance for the first time");
        options.add(this.fullDownloadRow);

        this.provisionJava = new JCheckBox("Provision Java if None is Found", true);
        this.provisionJava.setOpaque(false);
        this.provisionJavaRow = CleanroomUI.optionRow(this.provisionJava, "Cleanroom needs Java 25. The launcher's own Java is not suitable");
        options.add(this.provisionJavaRow);

        this.instanceNameField = new JTextField(this.seed == null ? "" : this.seed.extra(MmcTarget.OPTION_INSTANCE_NAME, ""));
        CleanroomUI.installTextFieldFocus(this.instanceNameField);
        this.instanceNameRow = fieldRow("Name", this.instanceNameField, "Leave empty for the default Cleanroom instance name");
        options.add(this.instanceNameRow);

        this.replaceJavaPath = new JCheckBox("Replace Java Path", false);
        this.replaceJavaPath.setOpaque(false);
        this.replaceJavaPathRow = CleanroomUI.optionRow(this.replaceJavaPath, "Points the instance's java path to the one the installer found");
        options.add(this.replaceJavaPathRow);

        this.installScripts = new JCheckBox("Write Launch Scripts", this.seed == null || !this.seed.flag(ServerTarget.OPTION_NO_SCRIPTS));
        this.installScripts.setOpaque(false);
        this.installScriptsRow = CleanroomUI.optionRow(this.installScripts, "Provides ready-made script with custom arguments support");
        options.add(this.installScriptsRow);

        this.fullDownloadRow.setVisible(ClientTarget.ID.equals(this.selectedTargetId));
        this.installScriptsRow.setVisible(ServerTarget.ID.equals(this.selectedTargetId));
        refreshMmcOptions();
        // Stays hidden until the background scan reports that no usable Java exists.
        this.provisionJavaRow.setVisible(false);

        return CleanroomUI.card("Options", "Extras to configure before installing", options);
    }

    private JPanel buttons() {
        JPanel footer = CleanroomUI.footer();

        JButton cancel = new JButton("Cancel");
        CleanroomUI.ghost(cancel);
        CleanroomUI.tooltip(cancel, "Close without installing (Esc)");
        cancel.addActionListener(event -> cancel());

        JButton install = new JButton("Install");
        CleanroomUI.primary(install);
        CleanroomUI.tooltip(install, "Install Cleanroom into the chosen folder (Enter)");
        this.frame.getRootPane().setDefaultButton(install);
        install.addActionListener(event -> startInstall());

        footer.add(cancel);
        footer.add(install);
        return footer;
    }

    private void onModeChanged(InstallTarget target) {
        this.modeDescription.setText(target.description());
        this.directoryField.setText(target.defaultDirectory(this.environment).toString());
        this.detected.setText(detectionText());
        this.fullDownloadRow.setVisible(ClientTarget.ID.equals(target.id()));
        this.installScriptsRow.setVisible(ServerTarget.ID.equals(target.id()));
        refreshMmcOptions();
    }

    /**
     * The mmc mode does two different things depending on where it is pointed, so the options it
     * offers follow the directory: a name for an instance about to be created, a java path for one
     * that already exists.
     */
    private void refreshMmcOptions() {
        boolean mmc = MmcTarget.ID.equals(this.selectedTargetId);
        boolean existing = mmc && MmcTarget.updatesExistingInstance(currentDirectory());
        this.instanceNameRow.setVisible(mmc && !existing);
        this.replaceJavaPathRow.setVisible(existing);
    }

    private Path currentDirectory() {
        return this.environment.path(this.directoryField.getText());
    }

    /** A labelled text field, laid out like the option rows it sits next to. */
    private static JPanel fieldRow(String label, JTextField field, String description) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel caption = CleanroomUI.fieldLabel(label);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(caption);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        row.add(field);
        JLabel detail = CleanroomUI.statusLabel(description);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(detail);
        return row;
    }

    private String detectionText() {
        if (MmcTarget.ID.equals(this.selectedTargetId)) {
            Path directory = currentDirectory();
            MmcInstance existing = MmcInstance.inspect(directory);
            switch (existing.kind()) {
                case CLEANROOM:
                    return "Existing " + existing.describe() + ". Repairs it or changes its version.";
                case FORGE:
                    return "Existing " + existing.describe() + ". Cleanroom will replace Forge in it."
                            + System.lineSeparator()
                            + "Consult after installation: https://cleanroommc.com/wiki/end-user-guide/preparing-your-modpack";
                case VANILLA:
                    return "Existing " + existing.describe() + ". Cleanroom will be added to it.";
                default:
                    break;
            }
            if (InstallLocations.instancesOfLauncherRoot(directory) != null) {
                return "This is a launcher folder. A new instance will be created in it.";
            }
            List<DetectedLauncher> launchers = InstallLocations.multiMcFamily(this.environment);
            return launchers.isEmpty()
                    ? "No Prism, PolyMC or MultiMC installation was found."
                    : "Found " + launchers.get(0).kind().displayName() + ". A new instance will be created here.";
        }
        if (ClientTarget.ID.equals(this.selectedTargetId)) {
            Path directory = this.environment.path(this.directoryField.getText());
            return InstallLocations.looksLikeMinecraft(directory)
                    ? "This looks like a Minecraft installation."
                    : "No Minecraft installation detected here yet.";
        }
        if (ServerTarget.ID.equals(this.selectedTargetId) && ServerTarget.busyDirectory(currentDirectory())) {
            return "This folder is not empty and holds no server installation."
                    + System.lineSeparator()
                    + "Installing here will add files to it.";
        }
        return "";
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(new File(this.directoryField.getText()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
            this.directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void directoryChanged() {
        if (this.detected == null || this.instanceNameRow == null) {
            return;
        }
        this.detected.setText(detectionText());
        refreshMmcOptions();
    }

    private Path defaultDirectory() {
        if (this.seed != null && this.seed.directory() != null) {
            return this.seed.directory();
        }
        try {
            return InstallTargets.byId(this.selectedTargetId).defaultDirectory(this.environment);
        } catch (InstallException e) {
            return this.environment.workingDirectory();
        }
    }

    private void cancel() {
        this.exitCode.set(ExitCode.CANCELLED.code());
        this.frame.dispose();
    }

    private void startInstall() {
        InstallRequest request = request();
        this.frame.setVisible(false);

        ProgressWindow progress = new ProgressWindow();
        progress.updateStatus("Preparing…");
        progress.show();
        SwingProgress listener = new SwingProgress(progress);

        new Thread(() -> {
            Log log = Log.toFile(this.environment.installerCache().resolve("logs")
                    .resolve("installer-" + System.currentTimeMillis() + ".log"));
            LogBridge.attach(log);
            try {
                InstallTarget target = InstallTargets.byId(request.targetId());
                Downloader downloader = new Downloader(log, request.offline());
                try (ProfileSource source = Sources.open(request.version(), downloader, log,
                        this.environment.installerCache(), listener)) {
                    InstallContext context = new InstallContext(source, downloader,
                            new JavaResolver(this.environment, log), this.environment, log)
                            .listener(listener);
                    target.validate(request, context);
                    InstallPlan plan = target.plan(request, context);
                    InstallResult result = target.apply(plan, context, listener);
                    progress.close();
                    succeeded(result, log);
                }
            } catch (InstallException e) {
                progress.close();
                log.error(e, "Install failed");
                failed(e.getMessage(), log, e.exitCode());
            } catch (RuntimeException e) {
                progress.close();
                log.error(e, "Unexpected failure");
                failed(String.valueOf(e), log, ExitCode.INTERNAL);
            } finally {
                LogBridge.detach();
                log.close();
            }
        }, "cleanroom-installer").start();
    }

    private InstallRequest request() {
        InstallRequest.Builder builder = (this.seed == null
                ? InstallRequest.builder(this.selectedTargetId)
                : this.seed.toBuilder())
                .directory(this.environment.path(this.directoryField.getText()).toAbsolutePath())
                .assumeYes(true);
        JavaSpec java = (this.seed == null ? JavaSpec.defaults() : this.seed.java())
                .withProvision(this.provisionJava.isSelected());
        builder.java(java);
        builder.version(this.selectedVersion);
        if (ClientTarget.ID.equals(this.selectedTargetId) && this.fullDownload.isSelected()) {
            builder.flag(ClientTarget.OPTION_FULL, true);
        }
        if (MmcTarget.ID.equals(this.selectedTargetId)) {
            if (this.instanceNameRow.isVisible()) {
                // An empty name is meaningful: it hands the naming back to the target.
                builder.extra(MmcTarget.OPTION_INSTANCE_NAME, this.instanceNameField.getText().trim());
            }
            builder.flag(MmcTarget.OPTION_REPLACE_JAVA_PATH,
                    this.replaceJavaPathRow.isVisible() && this.replaceJavaPath.isSelected());
        }
        if (ServerTarget.ID.equals(this.selectedTargetId)) {
            builder.flag(ServerTarget.OPTION_PIN_JAVA, this.provisionJava.isSelected());
            builder.flag(ServerTarget.OPTION_NO_SCRIPTS, !this.installScripts.isSelected());
        }
        // The builder keeps the seed's target id, so switching modes in the window has to be applied.
        InstallRequest built = builder.build();
        return this.selectedTargetId.equals(built.targetId())
                ? built
                : rebuild(built, this.selectedTargetId);
    }

    private static InstallRequest rebuild(InstallRequest original, String targetId) {
        InstallRequest.Builder builder = InstallRequest.builder(targetId)
                .version(original.version())
                .directory(original.directory())
                .offline(original.offline())
                .dryRun(original.dryRun())
                .force(original.force())
                .assumeYes(original.assumeYes())
                .java(original.java())
                .jvmArgs(original.jvmArgs());
        for (Map.Entry<String, String> extra : original.extras().entrySet()) {
            builder.extra(extra.getKey(), extra.getValue());
        }
        return builder.build();
    }

    private void succeeded(InstallResult result, Log log) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder message = new StringBuilder();
            message.append(result.isNoOp()
                    ? "Everything was already in place"
                    : "Installed into " + result.root());
            for (String note : result.notes()) {
                message.append(System.lineSeparator()).append(note);
            }
            if (log.file() != null) {
                message.append(System.lineSeparator()).append("Log: ").append(log.file());
            }
            CleanroomUI.showInfo(null, "Cleanroom installed", message.toString());
            this.exitCode.set(ExitCode.SUCCESS.code());
            this.frame.dispose();
        });
    }

    private void failed(String message, Log log, ExitCode code) {
        SwingUtilities.invokeLater(() -> {
            String detail = message + (log.file() == null ? "" : System.lineSeparator() + "Log: " + log.file());
            CleanroomUI.showError(null, "Install failed", detail);
            this.exitCode.set(code.code());
            this.frame.setVisible(true);
        });
    }

}
