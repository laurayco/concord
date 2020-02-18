package com.walmartlabs.concord.runtime.v2.runner.compiler;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.runtime.v2.model.ProcessDefinition;
import com.walmartlabs.concord.runtime.v2.model.Step;
import com.walmartlabs.concord.runtime.v2.sdk.Compiler;
import com.walmartlabs.concord.svm.Command;
import org.eclipse.sisu.Typed;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

@Named
@Typed
public class DefaultCompiler implements Compiler {

    private final Collection<StepCompiler<?>> compilers;

    @Inject
    public DefaultCompiler(Collection<StepCompiler<?>> compilers) {
        this.compilers = compilers;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Command compile(ProcessDefinition processDefinition, Step step) {
        StepCompiler sc = compilers.stream().filter(c -> c.accepts(step))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find a compiler for " + step.getClass()));

        CompilerContext ctx = new DefaultCompilerContext(this, processDefinition);
        return sc.compile(ctx, step);
    }

    public static class DefaultCompilerContext implements CompilerContext {

        private final Compiler compiler;
        private final ProcessDefinition processDefinition;

        private DefaultCompilerContext(Compiler compiler, ProcessDefinition processDefinition) {
            this.compiler = compiler;
            this.processDefinition = processDefinition;
        }

        @Override
        public Compiler compiler() {
            return compiler;
        }

        @Override
        public ProcessDefinition processDefinition() {
            return processDefinition;
        }
    }
}
