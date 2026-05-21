from django.shortcuts import render
from django.http import JsonResponse, HttpResponseNotAllowed
import random
import string

# Create your views here.

def get_users(request):
        
    queryParams = request.GET.dict()

    if "payload_size" in queryParams:
        payload_size = int(queryParams["payload_size"])
    else:
        payload_size = 10

    if "how_many" in queryParams:
        how_many = int(queryParams["how_many"])
    else:
        how_many = 1
    
    if "has_next" in queryParams:
        has_next = queryParams["has_next"]
    else:
        has_next = "true"

    if "has_error" in queryParams:
        has_error = queryParams["has_error"]
    else:
        has_error = "false"

    list = []
    if has_next == "true" and how_many > 0 and has_error == "false":
        for i in range(how_many):
            dic = {}
            dic["user_id"] = generateRandom(payload_size)
            dic["user_name"] = generateRandom(payload_size)
            list.append(dic)

        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"users":list}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })

    elif has_next == "false" and has_error == "false":
        dic = {}
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"users":list}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })
    elif has_error == "true":
        dic = {}
        dic["error"] = "true"
        dic["error_message"] = "An error has occurred"
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"users":list}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
            })
    else:
        pass

def get_posts(request):

    queryParams = request.GET.dict()

    if "payload_size" in queryParams:
        payload_size = int(queryParams["payload_size"])
    else:
        payload_size = 10

    if "user_id" in queryParams:
        user_id = queryParams["user_id"]
    else:
        user_id = 33333333

    if "how_many" in queryParams:
        how_many = int(queryParams["how_many"])
    else:
        how_many = 1
    
    if "has_next" in queryParams:
        has_next = queryParams["has_next"]
    else:
        has_next = "true"

    if "has_error" in queryParams:
        has_error = queryParams["has_error"]
    else:
        has_error = "false"

    list = []
    if has_next == "true" and how_many > 0 and has_error == "false":
        for i in range(how_many):
            dic = {}
            dic["post_id"] = generateRandom(payload_size)
            dic["post_title"] = generateRandom(payload_size)
            dic["post_text"] = generateRandom(payload_size)
            list.append(dic)

        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"posts":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })

    elif has_next == "false" and has_error == "false":
        dic = {}
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"posts":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })
    elif has_error == "true":
        dic = {}
        dic["error"] = "true"
        dic["error_message"] = "An error has occurred"
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"posts":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
            })
    else:
        pass



def get_post_comments(request):

    queryParams = request.GET.dict()

    if "payload_size" in queryParams:
        payload_size = int(queryParams["payload_size"])
    else:
        payload_size = 10

    if "user_id" in queryParams:
        user_id = queryParams["user_id"]
    else:
        user_id = 33333333
    
    if "post_id" in queryParams:
        post_id = queryParams["post_id"]
    else:
        post_id = 555555

    if "how_many" in queryParams:
        how_many = int(queryParams["how_many"])
    else:
        how_many = 1
    
    if "has_next" in queryParams:
        has_next = queryParams["has_next"]
    else:
        has_next = "true"

    if "has_error" in queryParams:
        has_error = queryParams["has_error"]
    else:
        has_error = "false"

    list = []
    if has_next == "true" and how_many > 0 and has_error == "false":
        for i in range(how_many):
            dic = {}
            dic["comment_id"] = generateRandom(payload_size)
            dic["comment_title"] = generateRandom(payload_size)
            dic["comment_text"] = generateRandom(payload_size)
            list.append(dic)

        return JsonResponse({
            'statusCode': 200,
            'body' :{"data":{"comments":list, "user_id":user_id, "post_id":post_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })

    elif has_next == "false" and has_error == "false":
        dic = {}
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"comments":list, "user_id":user_id, "post_id":post_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })
    elif has_error == "true":
        dic = {}
        dic["error"] = "true"
        dic["error_message"] = "An error has occurred"
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"comments":list, "user_id":user_id, "post_id":post_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
            })
    else:
        pass


def get_post_attachements(request):

    queryParams = request.GET.dict()

    if "payload_size" in queryParams:
        payload_size = int(queryParams["payload_size"])
    else:
        payload_size = 10

    if "user_id" in queryParams:
        user_id = queryParams["user_id"]
    else:
        user_id = 33333333
    
    if "post_id" in queryParams:
        post_id = queryParams["post_id"]
    else:
        post_id = 555555

    if "how_many" in queryParams:
        how_many = int(queryParams["how_many"])
    else:
        how_many = 1
    
    if "has_next" in queryParams:
        has_next = queryParams["has_next"]
    else:
        has_next = "true"

    if "has_error" in queryParams:
        has_error = queryParams["has_error"]
    else:
        has_error = "false"

    list = []
    if has_next == "true" and how_many > 0 and has_error == "false":
        for i in range(how_many):
            dic = {}
            dic["attachment_id"] = generateRandom(payload_size)
            dic["attachment_title"] = generateRandom(payload_size)
            dic["attachment_text"] = generateRandom(payload_size)
            list.append(dic)

        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"attachments":list, "user_id":user_id, "post_id":post_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })

    elif has_next == "false" and has_error == "false":
        dic = {}
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"attachments":list, "user_id":user_id, "post_id":post_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })
    elif has_error == "true":
        dic = {}
        dic["error"] = "true"
        dic["error_message"] = "An error has occurred"
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"attachments":list, "user_id":user_id, "post_id":post_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
            })
    else:
        pass


def get_user_communities(request):

    queryParams = request.GET.dict()

    if "payload_size" in queryParams:
        payload_size = int(queryParams["payload_size"])
    else:
        payload_size = 10

    if "user_id" in queryParams:
        user_id = queryParams["user_id"]
    else:
        user_id = 33333333

    if "how_many" in queryParams:
        how_many = int(queryParams["how_many"])
    else:
        how_many = 1
    
    if "has_next" in queryParams:
        has_next = queryParams["has_next"]
    else:
        has_next = "true"

    if "has_error" in queryParams:
        has_error = queryParams["has_error"]
    else:
        has_error = "false"

    list = []
    if has_next == "true" and how_many > 0 and has_error == "false":
        for i in range(how_many):
            dic = {}
            dic["community_id"] = generateRandom(payload_size)
            dic["community_title"] = generateRandom(payload_size)
            dic["community_description"] = generateRandom(payload_size)
            list.append(dic)

        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"communities":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })

    elif has_next == "false" and has_error == "false":
        dic = {}
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"communities":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
        })
    
    elif has_error == "true":
        dic = {}
        dic["error"] = "true"
        dic["error_message"] = "An error has occurred"
        list.append(dic)
        return JsonResponse({
            'statusCode': 200,
            'body' : {"data":{"communities":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error}
            })
    
    else:
        pass



def generateRandom(payload_size):
    random_string = ''.join(random.choices(string.ascii_letters + string.digits, k=payload_size))
    return random_string
