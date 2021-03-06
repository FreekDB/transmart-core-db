package org.transmartproject.db.dataquery.highdim.metabolite

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

@EqualsAndHashCode(includes = 'assay,annotation')
class DeSubjectMetabolomicsData implements Serializable {

    BigDecimal zscore
    BigDecimal rawIntensity
    BigDecimal logIntensity
    DeMetaboliteAnnotation jAnnotation

    static belongsTo = [
            assay:      DeSubjectSampleMapping,
            annotation: DeMetaboliteAnnotation,
    ]

    static mapping = {
        table      schema:    'deapp'
        id         composite: ['assay', 'annotation']

        assay      column:    'assay_id'
        annotation column:    'metabolite_annotation_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jAnnotation column: 'metabolite_annotation_id', updateable: false, insertable: false
        version false
    }

    static constraints = {
        zscore scale: 5
        rawIntensity nullable: true, scale: 5
        logIntensity nullable: true, scale: 5
    }
}
