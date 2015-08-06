/*
 * Copyright 2015 Daniel Gredler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.gredler.testfs;

/**
 * <p>
 * File permissions (combinations of read, write and/or execute).
 *
 * <p>
 * No distinction is made between owner / group / other.
 */
public enum Permissions {

    /** Allow read, write and execute. */
    RWX,

    /** Allow read and write, but not execute. */
    RW_,

    /** Allow read and execute, but not write. */
    R_X,

    /** Allow read, but not write or execute. */
    R__,

    /** Allow write and execute, but not read. */
    _WX,

    /** Allow write, but not read or execute. */
    _W_,

    /** Allow execute, but not read or write. */
    __X,

    /** Don't allow any access. */
    ___
}
