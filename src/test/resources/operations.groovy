import groovy.json.JsonSlurper


operations {
    
    operation {

        name "github_employee" 
        type "HTTP"
        method "GET"
        host  "https://api.jsonbin.io"
        headers "X-Master-Key": '$2a$10$LrPVM0vD/9GZJerxV2.u5uCblZ6ZnYJOVPojOs6UL562cc2GMcov6', "X-Bin-Meta": "false"
        path  "/v3/b/6ae86c41ac6210605a658274"
    }

    operation {

        name "github_employee_operation_variables" 
        type "HTTP"
        method "GET"
        host  "{{host}}"
        headers "X-Master-Key": '$2a$10$LrPVM0vD/9GZJerxV2.u5uCblZ6ZnYJOVPojOs6UL562cc2GMcov6', "X-Bin-Meta": "false"
        path  "/v3/b/6ae86c41ac6210605a658274"
    }

    operation {

        name "compare_and_match" 
        type "NON_HTTP"
        from_value "{{from_value}}"
        to_value "{{to_value}}"
    }


    operation {

        name "open_product_api" 
        type "HTTP"
        method "GET"
        host  "https://dummyjson.com/"
        path  "products"
        query "limit": "3", "select": "id,title"
    }

    operation {

        name "paginated_open_product_api" 
        type "HTTP"
        method "GET"
        host  "https://dummyjson.com/"
        path  "products"
        query "limit": "{{limit}}", "skip": "{{skip}}", "select": "id,title"
    }


    operation {
        name "encrich_company_information" 
        type "NON_HTTP"
        basic_info "{{basic_info}}"
    }

    operation {
        name "encrich_personal_information" 
        type "NON_HTTP"
        encriched_info "{{encriched_info}}"
    }


    operation {

        name "step1" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many=2"
    }

    operation {

        name "step2" 
        type "HTTP"
        method "GET"
        host  "http://django:3000"
        path  "/users?how_many={{how_many}}"
        headers "how_many": "{{how_many}}" 
    }
}