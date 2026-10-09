/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target;

import com.cleanroommc.installer.java.JavaResolution;
import com.cleanroommc.installer.java.JavaSpec;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.util.Cancellation;
import com.cleanroommc.installer.util.ProgressListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractInstallTarget implements InstallTarget {

    protected static Set<Capability> capabilitySet(Capability... capabilities) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(capabilities)));
    }

    /**
     * The Java a plan refers to, or null on a dry run that would have to download one.
     */
    protected static JavaResolution resolveJava(
        InstallRequest request,
        InstallContext context,
        JavaSpec spec,
        InstallPlan plan
    ) throws InstallException {
        boolean explicit = spec.path() != null && !spec.path().isEmpty();
        if (request.dryRun() &&
            !explicit &&
            spec.allowProvision() &&
            !context.javaResolver().hasLocalJava(spec, context.listener())) {
            plan.note("Java " + spec.target() + " would be downloaded into " + context.env().javaCache());
            return null;
        }
        return context.javaResolver().resolve(spec, context.listener());
    }

    @Override
    public InstallResult apply(InstallPlan plan, InstallContext context, ProgressListener listener)
        throws InstallException {
        context.listener(listener);
        InstallResult result = new InstallResult(plan.root());
        List<Action> actions = plan.actions();
        if (actions.isEmpty()) {
            return result.noOp(true);
        }
        int done = 0;
        for (Action action : actions) {
            Cancellation.check(listener);
            context.log().debug(action.describe());
            action.execute(context);
            if (action.destination() != null) {
                result.wrote(action.destination());
            }
            done++;
            if (listener != null) {
                listener.progress(done, actions.size());
            }
        }
        for (String note : plan.notes()) {
            result.note(note);
        }
        return result;
    }

}
