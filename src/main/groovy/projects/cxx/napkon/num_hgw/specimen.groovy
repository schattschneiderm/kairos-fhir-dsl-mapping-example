package projects.cxx.napkon.num_hgw

import de.kairos.centraxx.fhir.r4.utils.FhirUrls
import de.kairos.fhir.centraxx.metamodel.IdContainer
import de.kairos.fhir.centraxx.metamodel.IdContainerType
import de.kairos.fhir.centraxx.metamodel.SampleIdContainer
import de.kairos.fhir.centraxx.metamodel.enums.SampleCategory
import de.kairos.fhir.centraxx.metamodel.enums.SampleKind

import java.text.SimpleDateFormat

import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.ID_CONTAINER_TYPE
import static de.kairos.fhir.centraxx.metamodel.AbstractIdContainer.PSN
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.ID_CONTAINER
import static de.kairos.fhir.centraxx.metamodel.AbstractSample.SAMPLE_CATEGORY
import static de.kairos.fhir.centraxx.metamodel.RootEntities.sample
/**
 * Represented by a CXX AbstractSample
 * From local IRB-CXX to NUM-CXX
 * @since v.1.9.0, CXX.v.3.18.2.3
 * @author Mario Schattschneider
 */

specimen {
    final String localOrgUnit = "NUM_SUEP"
    final String numOrgUnit = "NUM_HGW"
    final String localStdSampleLocation = "Universitätsmedizin Greifswald --> Proben-Eingang"
    final String numSampleLocation = "NUM --> HGW Greifswald --> PP Lager RT"

    final String localPatientIdContainerType = "MPI"
    final String numPatientIdContainerType = "LIMSPSN"

    // 1. Filter sample category
    // Export master samples
    final SampleCategory category = context.source[sample().sampleCategory()] as SampleCategory
    final boolean containsCategory = [SampleCategory.MASTER].contains(category)

    if (!containsCategory) {
        return
    }

    // Filter only samples with NUM orgunit
    if (numOrgUnit == context.source[sample().organisationUnit().code()] ) {
        id = "Specimen/" + context.source[sample().id()]
    } else {
        return
    }



    final def idContainer = context.source[ID_CONTAINER]?.find {
        "SAMPLEID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
    }

    final def idContainerBasis = context.source[ID_CONTAINER]?.find {
        "BASISSETID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
    }

    final def idContainerStudy = context.source[ID_CONTAINER]?.find {
        "STUDYSETID" == it[ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
    }

    extension {
        url = FhirUrls.Extension.UPDATE_WITH_OVERWRITE
        valueBoolean = false
    }

    if (idContainer) {
        identifier {
            value = "NUM_" + idContainer[PSN]
            type {
                coding {
                    system = "urn:centraxx"
                    code = "SAMPLEID"
                }
            }
        }

        identifier {
            value = idContainer[PSN]
            type {
                coding {
                    system = "urn:centraxx"
                    code = "NUM_SAMPLEID"
                }
            }
        }
    }

    if (idContainerBasis || idContainerStudy) {
        identifier {
            if (idContainerBasis) {
                value = idContainerBasis[PSN]
            }
            if (idContainerStudy) {
                value = idContainerStudy[PSN]
            }
            type {
                coding {
                    system = "urn:centraxx"
                    code = "SETID"
                }
            }
        }
    }

    //2:  reposition date.


    if (context.source[sample().repositionDate()]) {
        extension {
            url = FhirUrls.Extension.Sample.REPOSITION_DATE
            valueDateTime = context.source[sample().repositionDate().date()]
        }
    }

    //3: location path
    // Master-Samples in Laboreingang

    if (context.source[sample().sampleLocation().locationPath()] as String == numSampleLocation) {
        extension {
            url = FhirUrls.Extension.Sample.SAMPLE_LOCATION
            extension {
                url = FhirUrls.Extension.Sample.SAMPLE_LOCATION_PATH
                valueString = localStdSampleLocation
            }
        }
    }

    // Standard organization unit attached to sample
    extension {
        url = FhirUrls.Extension.Sample.ORGANIZATION_UNIT
        valueReference {
            // by identifier
            identifier {
                value = localOrgUnit
            }
        }
    }

    status = context.source[sample().restAmount().amount()] > 0 ? "available" : "unavailable"

    //  sample type mapping
    type {
        coding {
            system = "urn:centraxx"
            code = toLocalType(
                    context.source[sample().sampleType().code()] as String
            )
        }
    }

    final def patIdContainer = context.source[sample().patientContainer().idContainer()]?.find {
        numPatientIdContainerType == it[IdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
    }

    if (patIdContainer) {
        subject {
            identifier {
                value = patIdContainer[IdContainer.PSN]
                type {
                    coding {
                        system = "urn:centraxx"
                        code = localPatientIdContainerType
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
                value = toLocalContainer(context.source[sample().sampleType().code()] as String)
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
                        code = toLocalProcessing(context.source[sample().stockProcessing().code()] as String)
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
                        code = toLocalProcessing(context.source[sample().secondProcessing().code()] as String)
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

static String toLocalType(final String sampleType) {
    //MASTER
    if (sampleType == "SER" ) return "SER" //Serum
    else if (sampleType == "EDTAWB" ) return "EDTABLD" //EDTA Vollblut
    else if (sampleType == "CIT" ) return "CIT" //Citrat
    else if (sampleType == "NUM_pbmc_cpt" ) return "PBMC_CPT" //PBMC CPT
    else if (sampleType == "NUM_liquor" ) return "LIQUOR" //
    else if (sampleType == "NUM_bal" ) return "BAL" //
    else if (sampleType == "URN" ) return "URN" //
    else if (sampleType == "NUM_rachenabstrich" ) return "PHS" //
    else if (sampleType == "NUM_nasen-rachenabstrich" ) return "NSPHS" //
    else if (sampleType == "NUM_speichel" ) return "Speichel" //
    else if (sampleType == "NUM_pax" ) return "PAX" //
    else if (sampleType == "NUM_enta" ) return "ENTA" //

    else return "Unbekannt (XXX)"
}

static String toLocalProcessing(final String sourceProcessing) {

    if (sourceProcessing == "NUM_BEGINN_ZENT") return "BEGINN_1_ZENTRIFUGATION"
    else if (sourceProcessing == "NUM_RT15min2000g") return "18_15_2000_B"
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

static String toLocalContainer(final String sampleType) {
    //MASTER
    // ToDo ORG Container noch klären
    if (sampleType == "SER") return "BD-SER" //Serum
    else if (sampleType == "EDTAWB" ) return "BD-EDTA" //EDTA Vollblut
    else if (sampleType == "CIT" ) return "BD-Vacutainer" //Citrat
    else if (sampleType == "NUM_pbmc_cpt" ) return "ORG" //
    else if (sampleType == "NUM_liquor" ) return "ORG" //
    else if (sampleType == "NUM_bal" ) return "ORG" //
    else if (sampleType == "URN" ) return "BD-URN" //
    else if (sampleType == "NUM_rachenabstrich" ) return "SWAB" //
    else if (sampleType == "NUM_nasen-rachenabstrich" ) return "SWAB" //
    else if (sampleType == "NUM_speichel" ) return "SWAB" //
    else if (sampleType == "NUM_pax" ) return "BD-PAX" //
    else if (sampleType == "NUM_enta" ) return "ORG" //
}
