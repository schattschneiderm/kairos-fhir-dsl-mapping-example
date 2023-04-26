package projects.cxx.hgw

import de.kairos.centraxx.fhir.r4.utils.FhirUrls
import de.kairos.fhir.centraxx.metamodel.IdContainer
import de.kairos.fhir.centraxx.metamodel.IdContainerType
import de.kairos.fhir.centraxx.metamodel.SampleIdContainer
import de.kairos.fhir.centraxx.metamodel.enums.SampleCategory
import de.kairos.fhir.centraxx.metamodel.enums.SampleKind
import java.lang.reflect.Method

import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.ID_CONTAINER_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.PSN
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.ID_CONTAINER
import static de.kairos.fhir.centraxx.metamodel.RootEntities.sample

import javax.sql.DataSource
import java.sql.ResultSet

/**
 * Represented by a CXX AbstractSample
 * From local CXX to NUM-CXX
 * @since v.1.9.0, CXX.v.3.18.2
 * @author Mario Schattschneider
 *
 */


final String localOrgUnit = "NUM_SUEP"
final String localPatientIdContainerType = "MPI"

// NUM
final String numOrgUnit = "NUM_HGW"
final String numPatientIdContainerType = "LIMSPSN"


specimen {
    // 1. Filter sample category
    // Export only mastersamples, aliquotgroups and aliquots

    final SampleCategory category = context.source[sample().sampleCategory()] as SampleCategory
    final boolean containsCategory = [SampleCategory.MASTER, SampleCategory.ALIQUOTGROUP, SampleCategory.DERIVED].contains(category)

    if (!containsCategory) {
        return
    }

    // Export only samples with localorgunit
    if (localOrgUnit == context.source[sample().organisationUnit().code()] || localOrgUnit == context.source[sample().parent().organisationUnit().code()]) {
        id = "Specimen/" + context.source[sample().id()]
    } else {
        return
    }

    //  Disable update with overwrite
    extension {
        url = FhirUrls.Extension.UPDATE_WITH_OVERWRITE
        valueBoolean = false
    }

    // FHIR-Data

    // Reference

    if (context.source[sample().parent()] != null) {
        parent {
            // Reference by identifier SampleId, because parent MasterSample might already exists in the target system
            final def extSampleIdParent = context.source[sample().parent().idContainer()]?.find {
                final def entry -> "NUM_SAMPLEID" == entry[SampleIdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
            }

            if (SampleCategory.MASTER == context.source[sample().parent().sampleCategory()] as SampleCategory && extSampleIdParent) {
                identifier {
                    type {
                        coding {
                            code = "SAMPLEID"
                        }
                    }
                    value = extSampleIdParent[SampleIdContainer.PSN]
                }
            } else {
                reference = "Specimen/" + context.source[sample().parent().id()]
            }
        }
    }


    // ID-Container
    def idContainer = null

    if (category == SampleCategory.MASTER) {
        idContainer = context.source[ID_CONTAINER]?.find {
            "NUM_SAMPLEID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
        }
    } else {
        idContainer = context.source[ID_CONTAINER]?.find {
            "SAMPLEID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
        }
    }

    if (idContainer) {
        identifier {
            value = idContainer[PSN]
            type {
                coding {
                    system = "urn:centraxx"
                    code = "SAMPLEID"
                }
            }
        }
    }

    // Reposition date

    if (context.source[sample().repositionDate()]) {
        extension {
            url = FhirUrls.Extension.Sample.REPOSITION_DATE
            valueDateTime = context.source[sample().repositionDate().date()]
        }
    }

    //3: location path

    if (context.source[sample().sampleLocation().locationPath()] != null) {

        final String sampleLocationOid = context.source[sample().sampleLocation().id()] as String
        final String locationPath = context.source[sample().sampleLocation().locationPath()] as String
        Float temperature = context.source[sample().sampleLocation().temperature()] as Float

        if (temperature == null) {
               temperature = loadParentTempFromDb(sampleLocationOid)
        }
        extension {
            url = FhirUrls.Extension.Sample.SAMPLE_LOCATION
            extension {
                url = FhirUrls.Extension.Sample.SAMPLE_LOCATION_PATH
                valueString = toNumLocation(category, locationPath, temperature)
            }
        }
    }

    // NUM organization unit attached to sample
    extension {
        url = FhirUrls.Extension.Sample.ORGANIZATION_UNIT
        valueReference {
            identifier {
                value = numOrgUnit
            }
        }
    }

    status = context.source[sample().restAmount().amount()] > 0 ? "available" : "unavailable"

    //  sample type mapping
    type {
        coding {
            system = "urn:centraxx"
            code = toNumTypeCode(category, context.source[sample().sampleType().code()] as String)
        }
    }

    final def patIdContainer = context.source[sample().patientContainer().idContainer()]?.find {
        localPatientIdContainerType == it[IdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
    }

    if (patIdContainer) {
        subject {
            identifier {
                value = patIdContainer[IdContainer.PSN]
                type {
                    coding {
                        system = "urn:centraxx"
                        code = numPatientIdContainerType
                    }
                }
            }
        }
    }

    if (context.source[sample().receiptDate()]) {
        receivedTime {
            date = context.source[sample().receiptDate().date()]
        }
    }
    collection {
        collectedDateTime {
            date = context.source[sample().samplingDate().date()]
            quantity {
                value = context.source[sample().initialAmount().amount()] as Number
                unit = context.source[sample().initialAmount().unit()]
                system = "urn:centraxx"
            }
        }
    }

    container {
        if (context.source[sample().receptable()]) {
            identifier {
                value = toNumContainer(category)
                system = "urn:centraxx"
            }

            capacity {
                value = context.source[sample().receptable().size()]
                unit = context.source[sample().restAmount().unit()]
                system = "urn:centraxx"
            }
        }

        specimenQuantity {
            value = context.source[sample().restAmount().amount()] as Number
            unit = context.source[sample().restAmount().unit()]
            system = "urn:centraxx"
        }
    }

    extension {
        url = FhirUrls.Extension.SAMPLE_CATEGORY
        valueCoding {
            system = "urn:centraxx"
            code = context.source[sample().sampleCategory()]
        }
    }

    if (context.source[sample().concentration()]) {
        extension {
            url = FhirUrls.Extension.Sample.CONCENTRATION
            valueQuantity {
                value = context.source[sample().concentration().amount()]
                unit = context.source[sample().concentration().unit()]
            }
        }
    }

// SPREC Extensions
    extension {
        url = FhirUrls.Extension.SPREC
        extension {
            url = FhirUrls.Extension.Sprec.USE_SPREC
            valueBoolean = context.source[sample().useSprec()]
        }



        //
        // SPREC LIQUID
        //
        if (SampleKind.LIQUID == context.source[sample().sampleKind()] as SampleKind) {
            if (context.source[sample().sprecPrimarySampleContainer()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SPREC_PRIMARY_SAMPLE_CONTAINER
                    valueCoding {
                        system = "urn:centraxx"
                        code = context.source[sample().sprecPrimarySampleContainer().code()]
                    }
                }
            }
            if (context.source[sample().sprecPreCentrifugationDelay()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SPREC_PRE_CENTRIFUGATION_DELAY
                    valueCoding {
                        system = "urn:centraxx"
                        code = context.source[sample().sprecPreCentrifugationDelay().code()]
                    }
                }
            }
            if (context.source[sample().sprecPreCentrifugationDelayDate()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SPREC_PRE_CENTRIFUGATION_DELAY_DATE
                    valueDateTime = context.source[sample().sprecPreCentrifugationDelayDate().date()]
                }
            }
            if (context.source[sample().sprecPostCentrifugationDelay()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SPREC_POST_CENTRIFUGATION_DELAY
                    valueCoding {
                        system = "urn:centraxx"
                        code = context.source[sample().sprecPostCentrifugationDelay().code()]
                    }
                }
            }
            if (context.source[sample().sprecPostCentrifugationDelayDate()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SPREC_POST_CENTRIFUGATION_DELAY_DATE
                    valueDateTime = context.source[sample().sprecPostCentrifugationDelayDate().date()]
                }
            }
            if (context.source[sample().stockProcessing()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.STOCK_PROCESSING
                    valueCoding {
                        system = "urn:centraxx"
                        code = toNumStockProcessing(context.source[sample().stockProcessing().code()] as String)
                    }
                }
            }
            if (context.source[sample().stockProcessingDate()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.STOCK_PROCESSING_DATE
                    valueDateTime = context.source[sample().stockProcessingDate().date()]
                }
            }
            if (context.source[sample().secondProcessing()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SECOND_PROCESSING
                    valueCoding {
                        system = "urn:centraxx"
                        code = toNumStockProcessing(context.source[sample().secondProcessing().code()] as String)
                    }
                }
            }
            if (context.source[sample().secondProcessingDate()]) {
                extension {
                    url = FhirUrls.Extension.Sprec.SECOND_PROCESSING_DATE
                    valueDateTime = context.source[sample().secondProcessingDate().date()]
                }
            }
        }
    }
}

/*static boolean isMoreThanNDaysAgo(String dateString, int days) {
    final Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(dateString)
    final long differenceInMillis = (FhirUrls.System.currentTimeMillis() - date.getTime())
    return TimeUnit.DAYS.convert(differenceInMillis, TimeUnit.MILLISECONDS) > days
}*/

static String toNumTypeCode(SampleCategory category, final String sampleType) {

    if (sampleType == "SER" ) return "SER"
        else if (sampleType == "CIT" ) return "CIT"
        else if (sampleType == "URN" ) return "URN"
        else if (sampleType == "PHS" ) return "NUM_rachenabstrich" //
        else if (sampleType == "NSPHS" ) return "NUM_nasen-rachenabstrich" //
        else if (sampleType == "Speichel" ) return "NUM_speichel" //
        else if (sampleType == "ENTA") return "NUM_enta" // Trachealsekret

    if (category == SampleCategory.MASTER) {
        if (sampleType == "EDTABLD" ) return "EDTAWB" //EDTA Vollblut
            else if (sampleType == "PBMC_CPT" ) return "NUM_pbmc_cpt" //PBMC CPT
            else if (sampleType == "LIQUOR" ) return "NUM_liquor" // Liquor
            else if (sampleType == "BAL" ) return "NUM_bal" // Bronchoalveoläre Lavage
            else if (sampleType == "PAX" ) return "NUM_pax" // PaxGene
            else if (sampleType == "ENTA" ) return "NUM_enta" // Trachealsekret
    } else {
        if (sampleType == "EDTA" ) return "EDTA" //EDTA Plasma
        else if (sampleType == "BUF") return "EDTABUF" // BuffyCoat
        else if (sampleType == "PBMC_PLASMA") return "NUM_CPT_PL" // PBMC-Plasma
        else if (sampleType == "PBMC_C") return "NUM_PBMC_C" // PBMC-Zellen
        else if (sampleType == "LIQUOR_C") return "NUM_liquorc" // Liquor-Zellen
        else if (sampleType == "LIQUOR_F") return  "NUM_liquorf" // Liquor-Überstand
        else if (sampleType == "URN_F") return "NUM_urinf" // Urin-Überstand
        else if (sampleType == "URN_S") return "NUM_urins" // Urin-Sediement
        else if (sampleType == "DNA") return "NUM_DNA" // DNA
        else if (sampleType == "BAL_C") return "NUM_balc" // Bronchoalveoläre Lavage-Zellen
        else if (sampleType == "BAL_F") return "NUM_balf" // Bronchoalveoläre Lavage-Überstand
        else if (sampleType == "RNA") return "NUM_RNA" // RNA
    }

    // Fallback
    return "Unbekannt (XXX)"
}

static String toNumStockProcessing(final String sourceProcessing) {

    if (sourceProcessing == "BEGINN_1_ZENTRIFUGATION") return "NUM_BEGINN_ZENT"
    else if (sourceProcessing == "18_15_2000_B") return "NUM_RT15min2000g"
    else if (sourceProcessing.startsWith("A")) return "Sprec-A"
    else if (sourceProcessing.startsWith("B")) return "Sprec-B"
    else if (sourceProcessing.startsWith("C")) return "Sprec-C"
    else if (sourceProcessing.startsWith("D")) return "Sprec-D"
    else if (sourceProcessing.startsWith("E")) return "Sprec-E"
    else if (sourceProcessing.startsWith("F")) return "Sprec-F"
    else if (sourceProcessing.startsWith("G")) return "Sprec-G"
    else if (sourceProcessing.startsWith("H")) return "Sprec-H"
    else if (sourceProcessing.startsWith("I")) return "Sprec-I"
    else if (sourceProcessing.startsWith("J")) return "Sprec-J"
    else if (sourceProcessing.startsWith("M")) return "Sprec-M"
    else if (sourceProcessing.startsWith("N")) return "Sprec-N"
    else if (sourceProcessing.startsWith("X")) return "Sprec-X"
    else if (sourceProcessing.startsWith("Z")) return "Sprec-Z"
    else return "Sprec-X"
}

static String toNumContainer(final SampleCategory category) {

    if (SampleCategory.MASTER == category) {
        return "ORG"
    }
    return "NUM_AliContainer"
}

static toNumLocation(SampleCategory category, String sampleLocation, Float temperature = null) {
    // Sample Locations
    final String localStdStorageLocation = "Universitätsmedizin Greifswald --> Proben-Eingang"
    final String localLabEntryStorage = "Universitätsmedizin Greifswald --> Forschungslabor-Eingang"

    final String numStdStorageLocation = "NUM --> HGW Greifswald --> PP Lager RT"
    final String numLabEntryStorage = "NUM --> HGW Greifswald --> FHIR-Importlager --> Laboreingang"
    final String numMinus20StorageLocation = "NUM --> HGW Greifswald --> FHIR-Importlager --> Importlager -20°C"
    final String numMinus80StorageLocation = "NUM --> HGW Greifswald --> FHIR-Importlager --> Importlager -80°C"
    final String numMinus196StorageLocation = "NUM --> HGW Greifswald --> FHIR-Importlager --> Importlager -196°C"
    final String numUnknownTemperatureStorageLocation = "NUM --> HGW Greifswald --> FHIR-Importlager --> Importlager Temperatur Unbekannt"

    if (temperature != null && temperature != 0.0) {

        switch (temperature) {
            case -20:
                return numMinus20StorageLocation
            case -80:
                return numMinus80StorageLocation
            case -196:
                return numMinus196StorageLocation
            default:
                return numUnknownTemperatureStorageLocation
        }
    }

    if (category == SampleCategory.MASTER) {
        switch (sampleLocation) {
            case localStdStorageLocation:
                return numStdStorageLocation
            case localLabEntryStorage:
                return numLabEntryStorage
            default:
                return numUnknownTemperatureStorageLocation
        }
    }
    return numUnknownTemperatureStorageLocation

}

static Float loadParentTempFromDb(final String oid) {
    final String sql = "SELECT SL_PARTENT.TEMPERATURE " +
            "FROM CENTRAXX_SAMPLELOCATION SL " +
            "INNER JOIN CENTRAXX_SAMPLELOCATION SL_PARTENT " +
            "ON SL.PARENT=SL_PARTENT.OID " +
            "WHERE SL.OID=" + oid

    final Float temperature =  queryFromDb(sql,  "TEMPERATURE")
    if (temperature && temperature == 0)
        return null
    return temperature
}

static Float queryFromDb(final String sql, final String column) {

    // path to jdbc driver, e.g. for MSSQL from https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server
    final def localFile = new File("C:/applications/MSSQL_Driver/mssql-jdbc-12.2.0.jre11.jar")
    final URLClassLoader cl = new URLClassLoader(localFile.toURI().toURL())

    // load data source class
    final Class beanClass = cl.loadClass("com.microsoft.sqlserver.jdbc.SQLServerDataSource")
    final DataSource dataSource = beanClass.getDeclaredConstructor().newInstance() as DataSource

    invoke(dataSource, "setURL", "jdbc:sqlserver://141.53.77.173:1433;databaseName=KAIROS_SPRING;encrypt=false")
    invoke(dataSource, "setUser", "fhirdatareader")
    invoke(dataSource, "setPassword", "password123#")

    final ResultSet rs = dataSource.getConnection()
            .createStatement()
            .executeQuery(sql)

    try {
        while (rs.next()) {
            return rs.getFloat(column)
        }
    }
    finally {
        rs.close()
    }
}


private static <T> void invoke(final DataSource dataSource, final String methodName, final T value) throws ReflectiveOperationException {
    Class<?> clazz = dataSource.getClass()
    while (null != clazz) {
        try {
            final Method method = clazz.getDeclaredMethod(methodName, value.getClass())
            method.invoke(dataSource, value)
            return
        }
        catch (NoSuchMethodException ignored) {
            clazz = clazz.getSuperclass()
        }
    }

    throw new NoSuchMethodException(dataSource.getClass() + "." + methodName + "(" + value.getClass() + ")")
}

