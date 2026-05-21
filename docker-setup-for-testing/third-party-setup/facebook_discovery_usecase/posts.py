import json
import random
import string

def lambda_handler(event, context):

    if event["queryStringParameters"] != None:
        queryParams = event["queryStringParameters"]
    else:
        queryParams = {}

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

        return {
            'statusCode': 200,
            'body' :json.dumps({"data":{"posts":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error})
        }

    elif has_next == "false" and has_error == "false":
        dic = {}
        list.append(dic)
        return {
            'statusCode': 200,
            'body' :json.dumps({"data":{"posts":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error})
        }
    elif has_error == "true":
        dic = {}
        dic["error"] = "true"
        dic["error_message"] = "An error has occurred"
        list.append(dic)
        return {
            'statusCode': 200,
            'body' :json.dumps({"data":{"posts":list, "user_id":user_id}, "has_next":has_next, "how_many":how_many, "has_error":has_error})
            }
    else:
        pass


def generateRandom(payload_size):
    random_string = ''.join(random.choices(string.ascii_letters + string.digits, k=payload_size))
    return random_string
