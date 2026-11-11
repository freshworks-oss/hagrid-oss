import java.time.LocalDateTime

models {
    
    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "github_employee"

        // desired attributes required in this model
        // attr "process_on" : "context._response.model.process_on"
        attr "api_data" : "context._response.model"
    }

    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "github_employee_no_meta"

        // desired attributes required in this model
        attr "api_data" : "context._response.model"

        transform {

            data ->

                println "before data: ${data}"
                data.api_data?.res_body?.remove("meta")
                println "after data: ${data}"
                return data
        }
    }

    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "github_employee_remove_element"

        // desired attributes required in this model
        attr "api_data" : "context._response.model"

        transform {

            data ->
                
                def filtered_employees = data.api_data?.res_body?.employees?.findAll { it.email != "####" }
                data.api_data?.res_body?.employees = filtered_employees
                return data
        }
    }

    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "compare_and_match"

        // desired attributes required in this model
        attr "is_match" : "context._response.model.is_match", "to_value" : "context._response.model.to_value"
        attr "from_value" : "context._response.model.from_value"
    }

    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "compare_and_match_error"

        // desired attributes required in this model
        attr "is_match" : "context._response.model.is_match", "to_value" : "context._response.model.to_value"
        attr "from_value" : "context._response.model.from_value"
        attr "any_error" : "context._response.model.any_error"
    }

    // API Model refers to the model which will render the API output
    step_model {
        name "open_product_api"
        attr "product" : "context._response.model"

        transform {

            data ->

                println "before data: ${data}"
                return data
        }
    }




    step_model {
        name "encrich_company_information"
        attr "encriched_data" : "context._response.model"
    }

    step_model {
        name "encrich_personal_information"
        attr "encriched_data" : "context._response.model"
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "step1"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"
        attr "how_many" : "context._response.model.how_many"
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "step2"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"
    }
}
