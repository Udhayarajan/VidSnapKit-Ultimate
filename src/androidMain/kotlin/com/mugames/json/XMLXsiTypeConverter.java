/*
 *    Copyright (c) 2023 Udhayarajan M
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.mugames.json;
/*
Public Domain.
*/

/**
 * Type conversion configuration interface to be used with xsi:type attributes.
 * <pre>
 * <b>XML Sample</b>
 * {@code
 *      <root>
 *          <asString xsi:type="string">12345</asString>
 *          <asInt xsi:type="integer">54321</asInt>
 *      </root>
 * }
 * <b>JSON Output</b>
 * {@code
 *     {
 *         "root" : {
 *             "asString" : "12345",
 *             "asInt": 54321
 *         }
 *     }
 * }
 *
 * <b>Usage</b>
 * {@code
 *      Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
 *      xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
 *          &#64;Override public String convert(final String value) {
 *              return value;
 *          }
 *      });
 *      xsiTypeMap.put("integer", new XMLXsiTypeConverter<Integer>() {
 *          &#64;Override public Integer convert(final String value) {
 *              return Integer.valueOf(value);
 *          }
 *      });
 * }
 * </pre>
 * @author kumar529
 * @param <T> return type of convert method
 */
public interface XMLXsiTypeConverter<T> {
    T convert(String value);
}
