/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Initial Developer of the Original Code is the
 * Ruud de Moor Centrum of the Open University.
 */
package nl.ou.rdmc.stats;

public class SystemLogger implements ILogger {

  public void error(String msg) {
    System.out.println(msg);
  }

  public void info(String msg) {
        System.out.println(msg);
  }

}
