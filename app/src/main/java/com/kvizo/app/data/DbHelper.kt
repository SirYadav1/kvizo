package com.kvizo.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, "kvizo.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                avatar_id INTEGER DEFAULT 1,
                status TEXT DEFAULT '',
                bio TEXT DEFAULT '',
                level INTEGER DEFAULT 1,
                xp INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL,
                is_active INTEGER DEFAULT 1
            )"""
        )
        db.execSQL(
            """CREATE TABLE quizzes (
                id TEXT PRIMARY KEY,
                profile_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                tags TEXT DEFAULT '',
                time_limit_seconds INTEGER DEFAULT NULL,
                status TEXT DEFAULT 'published',
                description TEXT DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                attempts_count INTEGER DEFAULT 0,
                average_score REAL DEFAULT 0.0,
                is_remote INTEGER DEFAULT 0,
                FOREIGN KEY (profile_id) REFERENCES profiles(id)
            )"""
        )
        db.execSQL("CREATE INDEX idx_quizzes_profile ON quizzes(profile_id)")
        db.execSQL(
            """CREATE TABLE questions (
                id TEXT PRIMARY KEY,
                quiz_id TEXT NOT NULL,
                question_text TEXT NOT NULL,
                option_a TEXT NOT NULL,
                option_b TEXT NOT NULL,
                option_c TEXT DEFAULT '',
                option_d TEXT DEFAULT '',
                correct_option TEXT NOT NULL,
                position INTEGER NOT NULL,
                is_bookmarked INTEGER DEFAULT 0,
                FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
            )"""
        )
        db.execSQL("CREATE INDEX idx_questions_quiz ON questions(quiz_id)")
        db.execSQL(
            """CREATE TABLE attempts (
                id TEXT PRIMARY KEY,
                quiz_id TEXT NOT NULL,
                profile_id INTEGER NOT NULL,
                score INTEGER NOT NULL,
                total_questions INTEGER NOT NULL,
                correct_answers INTEGER NOT NULL,
                time_taken_seconds INTEGER NOT NULL,
                xp_earned INTEGER NOT NULL,
                attempted_at INTEGER NOT NULL,
                FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
                FOREIGN KEY (profile_id) REFERENCES profiles(id)
            )"""
        )
        db.execSQL("CREATE INDEX idx_attempts_profile ON attempts(profile_id)")
        db.execSQL("CREATE INDEX idx_attempts_quiz ON attempts(quiz_id)")
        db.execSQL(
            """CREATE TABLE attempt_answers (
                id TEXT PRIMARY KEY,
                attempt_id TEXT NOT NULL,
                question_id TEXT NOT NULL,
                selected_option TEXT NOT NULL,
                is_correct INTEGER NOT NULL,
                FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
                FOREIGN KEY (question_id) REFERENCES questions(id)
            )"""
        )
        db.execSQL("CREATE INDEX idx_attempt_answers_attempt ON attempt_answers(attempt_id)")
        db.execSQL(
            """CREATE TABLE badges (
                id TEXT PRIMARY KEY,
                profile_id INTEGER NOT NULL,
                badge_code TEXT NOT NULL,
                badge_name TEXT NOT NULL,
                unlocked_at INTEGER NOT NULL,
                FOREIGN KEY (profile_id) REFERENCES profiles(id),
                UNIQUE(profile_id, badge_code)
            )"""
        )
        db.execSQL(
            """CREATE TABLE daily_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                profile_id INTEGER NOT NULL,
                date TEXT NOT NULL,
                quizzes_attempted INTEGER DEFAULT 0,
                questions_answered INTEGER DEFAULT 0,
                correct_answers INTEGER DEFAULT 0,
                time_spent_seconds INTEGER DEFAULT 0,
                xp_earned INTEGER DEFAULT 0,
                FOREIGN KEY (profile_id) REFERENCES profiles(id),
                UNIQUE(profile_id, date)
            )"""
        )
        db.execSQL("CREATE INDEX idx_daily_stats_profile_date ON daily_stats(profile_id, date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE quizzes ADD COLUMN description TEXT DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE quizzes ADD COLUMN is_remote INTEGER DEFAULT 0")
        }
    }
}
