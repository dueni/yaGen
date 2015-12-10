/*
 Copyright 2014 Georg Kohlweiss

 Licensed under the Apache License, Version 2.0 (the License);
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an AS IS BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.github.gekoh.yagen.hibernate;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;

import java.util.Collection;

/**
 * @author Georg Kohlweiss 
 */
public interface DDLEnhancer {
    void initDDLEnhancer(Object profile, Dialect dialect, Collection<PersistentClass> persistentClasses);
    Object getDDLEnhancer();
}