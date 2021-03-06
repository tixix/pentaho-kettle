/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.metainject;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;
import org.pentaho.di.trans.steps.csvinput.CsvInputMeta;

public class MetaInjectTest extends TestCase {

  /**
   * Try to inject a few fields
   *
   * @throws Exception
   */
  public void testMetaInjectCsvInput() throws Exception {
    KettleEnvironment.init();
    TransMeta transMeta = new TransMeta( "test/org/pentaho/di/trans/steps/metainject/read_csv_file.ktr" );
    String[][] fields =
      new String[][] {
        new String[] { "id", "Integer", null, "3", "0", null, ",", ".", "left", },
        new String[] { "name", "String", null, "10", null, null, null, null, "none", },
        new String[] { "firstname", "String", null, "13", null, null, null, null, "none", },
        new String[] { "zip", "String", null, "5", null, null, null, null, "left", },
        new String[] { "city", "String", null, "8", null, null, null, null, "none", },
        new String[] { "birthdate", "Date", "yyyy/MM/dd", "10", null, null, null, null, "none", },
        new String[] { "street", "String", null, "11", null, null, null, null, "none", },
        new String[] { "housenr", "Integer", null, "3", "0", null, ",", ".", "left", },
        new String[] { "stateCode", "String", null, "9", null, null, null, null, "none", },
        new String[] { "state", "String", null, "30", null, null, null, null, "none", }, };
    String[] fieldKeys =
      new String[] {
        "FIELD_NAME", "FIELD_TYPE", "FIELD_FORMAT", "FIELD_LENGTH", "FIELD_PRECISION", "FIELD_CURRENCY",
        "FIELD_DECIMAL", "FIELD_GROUP", "FIELD_TRIM_TYPE", };
    String filename = "test/org/pentaho/di/trans/steps/metainject/customers-100.txt";

    String stepname = "CSV file input";
    StepMeta stepMeta = transMeta.findStep( stepname );
    CsvInputMeta csvInputMeta = (CsvInputMeta) stepMeta.getStepMetaInterface();
    StepMetaInjectionInterface injectionInterface = csvInputMeta.getStepMetaInjectionInterface();
    List<StepInjectionMetaEntry> entries = injectionInterface.getStepInjectionMetadataEntries();
    List<StepInjectionMetaEntry> injection = new ArrayList<StepInjectionMetaEntry>();

    // Inject the filename...
    //
    StepInjectionMetaEntry filenameEntry = findMetaEntry( entries, "FILENAME" );
    assertNotNull( filenameEntry );
    filenameEntry.setValue( filename );
    injection.add( filenameEntry );

    // Inject the fields too...
    //
    StepInjectionMetaEntry fieldsEntry = findMetaEntry( entries, "FIELDS" );
    assertNotNull( fieldsEntry );
    StepInjectionMetaEntry fieldEntry = fieldsEntry.getDetails().get( 0 );

    StepInjectionMetaEntry fieldsCopy = fieldsEntry.clone();
    fieldsCopy.setDetails( new ArrayList<StepInjectionMetaEntry>() );
    injection.add( fieldsCopy );

    for ( String[] field : fields ) {
      StepInjectionMetaEntry fieldCopy = fieldEntry.clone();
      fieldCopy.setDetails( new ArrayList<StepInjectionMetaEntry>() );

      for ( int i = 0; i < fieldKeys.length; i++ ) {
        StepInjectionMetaEntry entry =
          new StepInjectionMetaEntry( fieldKeys[i], field[i], ValueMetaInterface.TYPE_STRING, "description" );
        fieldCopy.getDetails().add( entry );
      }

      fieldsCopy.getDetails().add( fieldCopy );
    }
    csvInputMeta.injectStepMetadataEntries( injection );

    // Verify the filename...
    assertEquals( filename, csvInputMeta.getFilename() );

    // Verify the fields...
    //
    assertEquals( 10, csvInputMeta.getInputFields().length );

    Trans trans = new Trans( transMeta );
    trans.execute( null );
    trans.waitUntilFinished();
    Result result = trans.getResult();

    assertEquals( 101, result.getNrLinesInput() );
    assertEquals( 100, result.getNrLinesWritten() );

    // Verify the output of various attributes
    //
    RowMetaInterface rowMeta = transMeta.getStepFields( stepname );
    for ( int i = 0; i < rowMeta.getValueMetaList().size(); i++ ) {
      ValueMetaInterface valueMeta = rowMeta.getValueMetaList().get( i );
      // Verify name
      assertEquals( fields[i][0], valueMeta.getName() );
      // Verify data type
      assertEquals( fields[i][1], valueMeta.getTypeDesc() );
      // Format
      assertEquals( fields[i][2], valueMeta.getConversionMask() );
      // length
      assertEquals( fields[i][3], valueMeta.getLength() == -1 ? null : Integer.toString( valueMeta.getLength() ) );
      // precision
      assertEquals( fields[i][4], valueMeta.getPrecision() == -1 ? null : Integer.toString( valueMeta
        .getPrecision() ) );
      // Currency symbol
      assertEquals( fields[i][5], valueMeta.getCurrencySymbol() );
      // Decimal symbol
      assertEquals( fields[i][6], valueMeta.getDecimalSymbol() );
      // Grouping symbol
      assertEquals( fields[i][7], valueMeta.getGroupingSymbol() );
      // Trim Type
      assertEquals( fields[i][8], ValueMeta.getTrimTypeCode( valueMeta.getTrimType() ) );
    }

  }

  private StepInjectionMetaEntry findMetaEntry( List<StepInjectionMetaEntry> entries, String key ) {
    for ( StepInjectionMetaEntry entry : entries ) {
      if ( entry.getKey().equals( key ) ) {
        return entry;
      }
    }
    return null;
  }
}
