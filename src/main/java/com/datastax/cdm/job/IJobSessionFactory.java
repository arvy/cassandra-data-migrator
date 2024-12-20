/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.cdm.job;

import com.datastax.cdm.properties.PropertyHelper;
import com.datastax.oss.driver.api.core.CqlSession;

public interface IJobSessionFactory<T> {
    public enum JobType {
        MIGRATE, VALIDATE, GUARDRAIL
    }

    AbstractJobSession<T> getInstance(CqlSession originSession, CqlSession targetSession, PropertyHelper propHelper);
}
