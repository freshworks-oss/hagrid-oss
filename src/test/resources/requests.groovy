import groovy.json.JsonSlurper


operations {

    operation {

        name "compare_and_match_operation" 
        type "NON_HTTP"
        from_value "{{from_value}}"
        to_value "{{to_value}}", "{{to_to_value}}"
    }

    operation {

        name "action1" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many=2"
    }

    operation {

        name "action2" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
        headers "how_many": "{{how_many}}" 
    }

    operation {

        name "action3" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
    }

    operation {

        name "action4" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
    }

    operation {

        name "action5" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
    }

    operation {

        name "action6" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}&has_error={{has_error}}"
    }

    operation {

        name "get_repo_operation" 
        method "GET"
        type "HTTP"
        sub_type "REST"
        host "https://api.github.com"
        path "/user/repos?per_page={{per_page}}"
        headers "Authorization" : "Bearer {{my_token}}"
        content_type "application/json"
        
    }

    operation {

        name "create_github_issue_operation" 
        method "GET"
        type "HTTP"
        host "https://api.github.com"
        path "/repos/{{owner}}/{{repo}}/issues"
        headers "Authorization" : "Bearer {{my_token}}"
        content_type "application/json"
        body "{{issue_body}}"
        
    }
}