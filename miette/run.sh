source .env.local
docker compose up -d

if [ "$1" = "--sync-prod" ]; then
    echo "--- Sync prod → local ---"

    echo "[1/2] DB : dump Railway → import local..."
    mysqldump \
        -h $PROD_DATABASE_HOST -P $PROD_DATABASE_PORT \
        -u $PROD_DATABASE_USER -p$PROD_DATABASE_PASSWORD \
        $PROD_DATABASE_NAME \
        | sed 's/utf8mb4_0900_ai_ci/utf8mb4_unicode_ci/g' \
        | sed 's/utf8mb4_0900_bin/utf8mb4_bin/g' \
        | sed 's/ \/\*!50100 WITH PARSER `ngram` \*\///g' \
        > /tmp/miette_prod.sql
    mysql \
        -h $DATABASE_HOST -P $DATABASE_PORT \
        -u $DATABASE_USER -p$DATABASE_PASSWORD \
        $DATABASE_NAME < /tmp/miette_prod.sql
    rm /tmp/miette_prod.sql

    echo "[2/2] Images : R2 → MinIO..."
    rclone sync r2:$PROD_STORAGE_BUCKET minio:$STORAGE_BUCKET

    echo "--- Sync terminée ---"
fi

mvn spring-boot:run
