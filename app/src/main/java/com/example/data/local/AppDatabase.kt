package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Offline SQLite database managed by Room with WAL (Write-Ahead Logging) enabled
 * for non-blocking concurrent reads and fast transactional batch writes.
 */
@Database(
    entities = [DeckEntity::class, CardEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CardStateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun flashcardDao(): FlashcardDao

    companion object {
        private const val DATABASE_NAME = "memoflow_sm2.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                seedInitialDataIfNeeded(instance)
                instance
            }
        }

        private fun seedInitialDataIfNeeded(database: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = database.flashcardDao()
                if (dao.getDeckCount() == 0) {
                    val now = System.currentTimeMillis()
                    val hourMs = 3_600_000L

                    // Deck 1: System Design & Architecture
                    val sysDeckId = dao.insertDeck(
                        DeckEntity(
                            name = "Системный дизайн и базы данных",
                            createdAt = now - 3 * hourMs
                        )
                    )
                    dao.insertCards(
                        listOf(
                            CardEntity(
                                deckId = sysDeckId,
                                front = "Зачем в SQLite / Room включать режим Write-Ahead Logging (WAL)?",
                                back = "WAL позволяет выполнять чтение и запись конкурентно без блокировки читающих потоков.\n\nИзменения сначала пишутся в отдельный WAL-файл, что ускоряет транзакции и предотвращает лаги UI при реактивных подписках Flow.",
                                repetitions = 2,
                                interval = 3,
                                easeFactor = 2.6,
                                nextReviewDate = now - hourMs, // Due today
                                state = CardState.REVIEW
                            ),
                            CardEntity(
                                deckId = sysDeckId,
                                front = "В чем ключевая разница между B-Tree и LSM-Tree индексами?",
                                back = "• B-Tree: оптимизирован для быстрого чтения (in-place обновления страниц на диске).\n• LSM-Tree (Log-Structured Merge-tree): оптимизирован для высокой пропускной способности записи (append-only в MemTable + фоновый Compaction в SSTables).",
                                repetitions = 1,
                                interval = 1,
                                easeFactor = 2.5,
                                nextReviewDate = now - 2 * hourMs, // Due today
                                state = CardState.LEARNING
                            ),
                            CardEntity(
                                deckId = sysDeckId,
                                front = "Как работает формула фактора легкости (Ease Factor) в алгоритме SuperMemo SM-2?",
                                back = "EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))\n\nГде q — оценка ответа (от 0 до 5). Минимальный порог EF ограничен значением 1.3, чтобы карточки не попадали в бесконечную петлю коротких интервалов.",
                                repetitions = 0,
                                interval = 0,
                                easeFactor = 2.5,
                                nextReviewDate = now,
                                state = CardState.NEW
                            ),
                            CardEntity(
                                deckId = sysDeckId,
                                front = "Почему пакетная вставка (Batch Insert) в SQLite должна выполняться внутри единой транзакции?",
                                back = "По умолчанию каждый INSERT в SQLite оборачивается в отдельную транзакцию с синхронизацией диска (fsync).\n\nЕдиная транзакция (@Transaction / batch.commit) делает fsync один раз в конце, ускоряя импорт 10 000 карточек с десятков секунд до ~150 мс.",
                                repetitions = 0,
                                interval = 0,
                                easeFactor = 2.5,
                                nextReviewDate = now,
                                state = CardState.NEW
                            )
                        )
                    )

                    // Deck 2: English for Engineers
                    val engDeckId = dao.insertDeck(
                        DeckEntity(
                            name = "Английский: IT и инженерная лексика",
                            createdAt = now - 2 * hourMs
                        )
                    )
                    dao.insertCards(
                        listOf(
                            CardEntity(
                                deckId = engDeckId,
                                front = "Idempotent operation",
                                back = "Идемпотентная операция — операция, повторное выполнение которой дает тот же результат, что и первое (например, HTTP PUT или DELETE).",
                                repetitions = 1,
                                interval = 1,
                                easeFactor = 2.5,
                                nextReviewDate = now - hourMs,
                                state = CardState.REVIEW
                            ),
                            CardEntity(
                                deckId = engDeckId,
                                front = "Backpressure (в реактивных потоках)",
                                back = "Механизм управления потоком данных, при котором потребитель (Consumer) сигнализирует источнику (Producer) о необходимости замедлить отправку событий.",
                                repetitions = 0,
                                interval = 0,
                                easeFactor = 2.5,
                                nextReviewDate = now,
                                state = CardState.NEW
                            ),
                            CardEntity(
                                deckId = engDeckId,
                                front = "Graceful degradation",
                                back = "Плавная деградация — способность системы сохранять базовую работоспособность даже при отказе отдельных компонентов или высокой нагрузке.",
                                repetitions = 0,
                                interval = 0,
                                easeFactor = 2.5,
                                nextReviewDate = now,
                                state = CardState.NEW
                            )
                        )
                    )

                    // Deck 3: Algorithms & Complexity
                    val algoDeckId = dao.insertDeck(
                        DeckEntity(
                            name = "Алгоритмы и структуры данных",
                            createdAt = now - hourMs
                        )
                    )
                    dao.insertCards(
                        listOf(
                            CardEntity(
                                deckId = algoDeckId,
                                front = "Амортизированная сложность добавления элемента в динамический массив (ArrayList / Vector)",
                                back = "O(1) амортизированно.\nВ худшем случае при расширении буфера в 2 раза — O(N), но копирование происходит редко.",
                                repetitions = 2,
                                interval = 4,
                                easeFactor = 2.6,
                                nextReviewDate = now - hourMs,
                                state = CardState.REVIEW
                            ),
                            CardEntity(
                                deckId = algoDeckId,
                                front = "Когда поиск в ширину (BFS) предпочтительнее поиска в глубину (DFS)?",
                                back = "При поиске кратчайшего пути в невзвешенном графе, так как BFS исследует вершины послойно по расстоянию от старта.",
                                repetitions = 0,
                                interval = 0,
                                easeFactor = 2.5,
                                nextReviewDate = now,
                                state = CardState.NEW
                            )
                        )
                    )
                }
            }
        }
    }
}
