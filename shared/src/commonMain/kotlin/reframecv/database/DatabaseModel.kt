package reframecv.database

import reframecv.domain.DomainModel

/** Marks database models and requires conversion to a domain model. */
interface DatabaseModel<out Domain : DomainModel> {
    fun toDomainModel(): Domain
}

/** Defines a common factory contract for creating database models from domain models. */
interface DatabaseModelFactory<Domain : DomainModel, out T : DatabaseModel<Domain>> {
    fun fromDomainModel(domainModel: Domain): T
}
