from django.urls import path
from . import views

urlpatterns = [
    # Match the base endpoint path only
    path('users', views.get_users, name='get_user_api'),
    path('posts', views.get_posts, name='get_post_api'),
    path('post_comments', views.get_post_comments, name='get_post_comments_api'),
    path('post_attachements', views.get_post_attachements, name='get_post_attachements_api'),
    path('user_communities', views.get_user_communities, name='get_user_communities_api'),
]