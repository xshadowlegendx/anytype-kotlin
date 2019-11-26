package com.agileburo.anytype.domain.database.interactor

import com.agileburo.anytype.domain.base.BaseUseCase
import com.agileburo.anytype.domain.base.Either
import com.agileburo.anytype.domain.database.FilterMock
import com.agileburo.anytype.domain.database.model.Filter

class GetFilters : BaseUseCase<List<Filter>, BaseUseCase.None>() {

    override suspend fun run(params: None): Either<Throwable, List<Filter>> = try {
        Either.Right(addDefaultFilters(FilterMock.filters.toMutableList()))
    } catch (e: Throwable) {
        Either.Left(e)
    }

    private fun addDefaultFilters(filters: MutableList<Filter>): List<Filter> {
        filters.add(0, FilterMock.FILTER_ALL)
        return filters
    }
}