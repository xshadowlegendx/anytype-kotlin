package com.agileburo.anytype.feature_editor.data

import com.agileburo.anytype.feature_editor.domain.Block

/**
 * Created by Konstantin Ivanov
 * email :  ki@agileburo.com
 * on 20.03.2019.
 */
interface BlockConverter {
    fun modelToDomain(model: BlockModel): Block
    fun domainToModel(block: Block): BlockModel
}

class BlockConverterImpl : BlockConverter {

    override fun modelToDomain(model: BlockModel) = Block(
        id = model.id,
        content = model.content,
        parentId = model.parentId
    )

    override fun domainToModel(block: Block) = BlockModel(
        id = block.id,
        content = block.content,
        parentId = block.parentId,
        children = mutableListOf()
    )
}