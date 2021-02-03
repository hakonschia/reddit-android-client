package com.example.hakonsreader.api.persistence

import androidx.room.*
import com.example.hakonsreader.api.model.RedditUserInfo

@Dao
interface RedditUserInfoDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userInfo: RedditUserInfo)

    @Update
    fun update(userInfo: RedditUserInfo)

    @Query("SELECT * FROM reddit_user_info WHERE userId=:id")
    fun getById(id: Int) : RedditUserInfo?


}