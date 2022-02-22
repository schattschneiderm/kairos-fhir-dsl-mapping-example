package projects.uscore

import de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue

import static de.kairos.fhir.centraxx.metamodel.AbstractCode.CODE
import static de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue.LABOR_VALUE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping

/**
 * Represents a CXX LaborMapping for the US Core Resource Profile: US Core CarePlan Profile.
 * Specified by http://hl7.org/fhir/us/core/StructureDefinition/us-core-careplan
 *
 * TODO: Work in progress!
 *
 * @author Mike Wähnert
 * @since v.1.14.0, CXX.v.2022.1.0
 */

carePlan {

  if ("bool" != context.source[laborMapping().laborFinding().laborMethod().code()]) {
    return
  }

  id = "CarePlan/" + context.source[laborMapping().laborFinding().id()]

  meta {
    profile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-careplan")
  }

  final def laborFindingLaborValue = context.source[laborMapping().laborFinding().laborFindingLaborValues()]
      .find { final lblv -> lblv[LABOR_VALUE][CODE] == "boolean" }

  identifier {
    value = laborFindingLaborValue[LaborFindingLaborValue.BOOLEAN_VALUE]
  }

}


