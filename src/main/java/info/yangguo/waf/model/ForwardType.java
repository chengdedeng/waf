/*
 * Copyright 2018-present yangguo@outlook.com
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
package info.yangguo.waf.model;

public enum ForwardType {
    HTTP,
    DUBBO,
    GRPC,
    THRIFT,
    SOFA;

    public static ForwardType getType(String name) {
        for (ForwardType type : ForwardType.values()) {
            if (type.name().equals(name))
                return type;
        }
        return null;
    }
}
