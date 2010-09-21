/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.elements.fields;

import com.manydesigns.elements.logging.LogUtil;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
*/
public class DefaultOptionProvider implements OptionProvider {
    public static final String copyright =
            "Copyright (c) 2005-2010, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected final Object[] values;
    protected final Object[][] valuesArray;
    protected final List<Object>[] optionsArray;
    protected boolean needsValidation;

    public final static Logger logger =
            LogUtil.getLogger(DefaultOptionProvider.class);

    //**************************************************************************
    // Static builders
    //**************************************************************************

    public static DefaultOptionProvider create(int fieldCount,
                                               Object[][] valuesArray) {
        return new DefaultOptionProvider(fieldCount, valuesArray);
    }

    public static DefaultOptionProvider create(ClassAccessor classAccessor,
                                               Collection<Object> objects) {
        PropertyAccessor[] keyProperties = classAccessor.getKeyProperties();
        int fieldsCount = keyProperties.length;
        Object[][] valuesArray = new Object[objects.size()][fieldsCount];
        int i = 0;
        for (Object current : objects) {
            Object[] values = new Object[fieldsCount];
            int j = 0;
            for (PropertyAccessor property : keyProperties) {
                try {
                    Object value = property.get(current);
                    values[j] = value;
                } catch (Throwable e) {
                    LogUtil.warningMF(logger,
                            "Could not access property: {0}",
                            e, property.getName());
                }
                j++;
            }
            valuesArray[i] = values;
            i++;
        }
        return new DefaultOptionProvider(fieldsCount, valuesArray);
    }

    //**************************************************************************
    // Constructor
    //**************************************************************************

    public DefaultOptionProvider(int fieldCount, Object[][] valuesArray) {
        this.valuesArray = valuesArray;
        values = new Object[fieldCount];
        //noinspection unchecked
        optionsArray = new List[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            optionsArray[i] = new ArrayList<Object>();
        }
        needsValidation = true;
    }


    //**************************************************************************
    // OptionProvider implementation
    //**************************************************************************

    public int getFieldCount() {
        return values.length;
    }

    public void setValue(int index, Object value) {
        values[index] = value;
        needsValidation = true;
    }

    public Object getValue(int index) {
        validate();
        return values[index];
    }

    public List<Object> getOptions(int index) {
        validate();
        return optionsArray[index];
    }

    //**************************************************************************
    // inetrnal-use methods
    //**************************************************************************


    protected void validate() {
        if (!needsValidation) {
            return;
        }

        needsValidation = false;
        resetOptionsArray();

        // normalize null in values (only null values after first null)
        boolean foundNull = false;
        for (int j = 0; j < getFieldCount(); j++) {
            if (foundNull) {
                values[j] = null;
            } else if (values[j] == null) {
                foundNull = true;
            }
        }

        boolean atLeastOneMatching = false;
        for (Object[] currentRow : valuesArray) {
            Object previousValue = null;
            boolean matching = true;
            for (int j = 0; j < getFieldCount(); j++) {
                Object cellValue = currentRow[j];
                Object value = values[j];
                if (value != null && !value.equals(cellValue)) {
                    matching = false;
                }
                List<Object> options = optionsArray[j];
                if (j == 0) {
                    if (!options.contains(cellValue)) {
                        options.add(cellValue);
                    }
                } else { // j > 0
                    Object constrainedValue = values[j-1];
                    if (constrainedValue != null
                            && constrainedValue.equals(previousValue)) {
                        if (!options.contains(cellValue)) {
                            options.add(cellValue);
                        }
                    }
                }
                previousValue = cellValue;
            }
            if (matching) {
                atLeastOneMatching = true;
            }
        }
        if (!atLeastOneMatching) {
            resetValues();
        }
    }

    public void resetOptionsArray() {
        for (int i = 0; i < values.length; i++) {
            optionsArray[i].clear();
        }
    }

    public void resetValues() {
        for (int i = 0; i < values.length; i++) {
            values[i] = null;
        }
    }

}
