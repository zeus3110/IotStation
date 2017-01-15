/*
 * Copyright 2017 zeus3110
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zeus3110.iotstation;

import android.os.AsyncTask;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 * Created by super on 2016/12/31.
 */

public class TweetAsyncTask extends AsyncTask<Void,Void,Void> {

    private String Err=null;
    private String TweetStr;
    TwitterFactory factory;
    Twitter twitter;

    public TweetAsyncTask(String data, TwitterFactory _fact){
        TweetStr = data;
        factory = _fact;
    }

    @Override
    public void onPreExecute() {
        twitter = factory.getInstance();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            twitter.updateStatus(TweetStr);
        } catch (TwitterException e) {

        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Void... v) {

    }

    @Override
    protected void onPostExecute(Void v) {

    }

}


