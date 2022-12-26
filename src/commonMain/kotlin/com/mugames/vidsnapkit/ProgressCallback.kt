package com.mugames.vidsnapkit

import com.mugames.vidsnapkit.dataholders.Result

/**
 * @author Udhaya
 * Created on 31-07-2022
 */

interface ProgressCallback{
    fun onProgress(result: Result)
}