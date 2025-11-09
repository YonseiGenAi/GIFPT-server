import os
from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "GIFPT_AI.settings")

app = Celery("GIFPT_AI")
app.config_from_object("django.conf:settings", namespace="CELERY")

# 환경변수 기본값 보강
app.conf.broker_url = os.environ.get("REDIS_URL", "redis://redis:6379/0")
app.conf.result_backend = os.environ.get("REDIS_URL", "redis://redis:6379/0")
app.conf.task_default_queue = os.environ.get("CELERY_QUEUE", "gifpt.default")

app.autodiscover_tasks()
