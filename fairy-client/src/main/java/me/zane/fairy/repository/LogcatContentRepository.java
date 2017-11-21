/*
 * Copyright (C) 2017 Zane.
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
package me.zane.fairy.repository;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import me.zane.fairy.api.ContentNetService;
import me.zane.fairy.db.LogcatDao;
import me.zane.fairy.db.LogcatDatabase;
import me.zane.fairy.resource.AppExecutors;
import me.zane.fairy.resource.ContentMergeSource;
import me.zane.fairy.vo.LogcatContent;

/**
 * Created by Zane on 2017/11/17.
 * Email: zanebot96@gmail.com
 */

public class LogcatContentRepository {
    private static volatile LogcatContentRepository instance;
    private static ContentMergeSource source;
    private static ContentNetService service;
    private static final LogcatDao logcatDao;
    private final AppExecutors executors;

    static {
        logcatDao = LogcatDatabase.getInstance().logcatDao();
    }

    private LogcatContentRepository(AppExecutors executors) {
        this.executors = executors;
    }

    public static LogcatContentRepository getInstance(@NonNull AppExecutors executors) {
        service = new ContentNetService();
        source = new ContentMergeSource(executors, logcatDao, service);
        if (instance == null) {
            synchronized (LogcatContentRepository.class) {
                if (instance == null) {
                    instance = new LogcatContentRepository(executors);
                }
            }
        }
        return instance;
    }

    public LiveData<LogcatContent> getLogcatContent(int id) {
        source.init(id);
        source.initData();
        return source.asLiveData();
    }

    public void fetchData(String options, String filter) {
        source.fetchFromNet();
        service.enqueue(options, filter);
    }

    public void stopFetch() {
        service.stop();
        executors.getDiskIO().execute(source::replaceDbData);
    }

    public void insertContent(LogcatContent content) {
        executors.getDiskIO().execute(() -> logcatDao.insertLogcatContent(content));
    }

    public void insertIfNotExits(LogcatContent content) {
        executors.getDiskIO().execute(() -> logcatDao.insertIfNotExits(content));
    }

    public void clearContent(LogcatContent content) {
        executors.getDiskIO().execute(() -> {
            logcatDao.updateLogcatContent(content);
            source.clearContent();
        });
    }
}