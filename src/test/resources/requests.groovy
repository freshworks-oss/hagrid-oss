import groovy.json.JsonSlurper


requests {

    request {

        name "compare_and_match_request" 
        type "NON_HTTP"
        from_value "{{from_value}}"
        to_value "{{to_value}}", "{{to_to_value}}"
    }

    request {

        name "action1" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many=2"
    }

    request {

        name "action2" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
        headers "how_many": "{{how_many}}" 
    }

    request {

        name "action3" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
    }

    request {

        name "action4" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
    }

    request {

        name "action5" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
    }

    request {

        name "action6" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}&has_error={{has_error}}"
    }

    request {

        name "get_repo_request" 
        method "GET"
        type "HTTP"
        sub_type "REST"
        host "https://api.github.com"
        path "/user/repos?per_page={{per_page}}"
        headers "Authorization" : "Bearer {{my_token}}"
        content_type "application/json"
        
    }

    request {

        name "create_github_issue_request" 
        method "GET"
        type "HTTP"
        host "https://api.github.com"
        path "/repos/{{owner}}/{{repo}}/issues"
        headers "Authorization" : "Bearer {{my_token}}"
        content_type "application/json"
        body "{{issue_body}}"
        
    }
}