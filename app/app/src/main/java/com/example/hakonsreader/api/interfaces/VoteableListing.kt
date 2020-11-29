package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.enums.VoteType

interface VoteableListing {

    val id: String
    val score: Int
    val isScoreHidden: Boolean

    fun getVoteType() : VoteType
    fun setVoteType(voteType: VoteType)

}