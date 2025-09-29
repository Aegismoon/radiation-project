# Radiation Monitoring Platform

## 📖 Описание
Проект состоит из двух основных модулей:

- **Radiation Simulator** – генератор событий радиационного фона. Публикует сообщения о радиационных источниках в Kafka-топик `radiation-events`.
- **Radiometer App** – модуль измерения и обработки. Подписывается на Kafka, парсит события, рассчитывает дозу и уровень опасности, сохраняет результаты в PostgreSQL.

Вспомогательные сервисы:
- **Postgres** – хранилище измерений и накопленных доз.
- **pgAdmin** – веб-интерфейс для работы с Postgres.
- **Kafka UI** – веб-интерфейс для отладки Kafka.
- **Zookeeper + Kafka** – брокер сообщений для взаимодействия между модулями.

---

## 🏗 Архитектура (высокоуровневая)

```
+----------------+       Kafka       +-----------------+       +------------+
| Radiation      |  -->  Topic       | Radiometer App  |  -->  | PostgreSQL |
| Simulator      |       radiation-  | (parser,        |       | DB         |
| (Generator)    |       events      |  processors,    |       |            |
+----------------+                   |  router)        |       +------------+
                                     +-----------------+
```

---

## ⚙️ Запуск

### 1. Запуск инфраструктуры
В корне проекта есть `docker-compose.yml`. Он поднимает:
- Kafka + Zookeeper
- PostgreSQL + pgAdmin
- Kafka UI

```bash
cd docker
docker compose up -d
```

После запуска:
- Kafka доступна на `localhost:29092`
- Postgres на `localhost:5432` (user: `radiation`, pass: `radiation`, db: `radiation`)
- Kafka UI: http://localhost:8080
- pgAdmin: http://localhost:5050

---

### 2. Сборка и запуск приложений

#### Radiation Simulator (генератор)
Публикует тестовые события в Kafka.

```bash
sbt "project simulator" "run"
```

#### Radiometer App (обработчик)
Читает события из Kafka, обрабатывает и пишет в Postgres.

```bash
sbt "project radiometer" "run"
```

---

### 3. Проверка

#### Kafka
Проверить содержимое топика:

```bash
docker compose exec kafka kafka-console-consumer       --bootstrap-server localhost:29092       --topic radiation-events       --from-beginning --max-messages 5
```

#### Postgres
Зайти в контейнер и проверить таблицы:

```bash
docker compose exec postgres psql -U radiation -d radiation -c "SELECT * FROM radiation_measurements LIMIT 10;"
docker compose exec postgres psql -U radiation -d radiation -c "SELECT * FROM radiation_doses LIMIT 10;"
```

---

## 📂 Структура проекта

```
project/
  build.sbt                # настройки сборки
  modules/
    simulator/             # генератор событий
    radiometer/            # обработчик событий
docker/
  docker-compose.yml       # инфраструктура (Kafka, Postgres, UI)
  postgres-init/001-init.sql # скрипт для инициализации таблиц
```

---

## 🔧 Используемые технологии
- **Scala 2.13 + Cats Effect + FS2 + FS2-Kafka** – реализация потоков обработки
- **Slick + PostgreSQL** – работа с БД
- **Circe** – парсинг JSON
- **Docker Compose** – инфраструктура (Kafka, Postgres, UI)  
