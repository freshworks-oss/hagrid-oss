import java.time.LocalDateTime

models {

        // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "compare_and_match_model"

        // desired attributes required in this model
        attr "is_match" : "context._response.model.is_match", "to_value" : "context._response.model.to_value"
        attr "from_value" : "context._response.model.from_value"
    }
    
    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "get_repo_model"

        // desired attributes required in this model
        attr "repo_name" : "_response.name", "is_private" : "_response.private"
        attr "owner_name" : "_response.owner.login"
    }
    
    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "action1"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"
        attr "how_many" : "context._response.model.how_many"
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "action2"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "action3"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"

        filter {

            /**
                Here I am checking if first character of the user_name is A-Z or 0-9
            */
            data -> 
                return data?.user_name ==~ /^[A-Z][0-9].*/
        }
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "action4"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"

        filter {

            /**
                Here I am checking if first character of the user_name is A-Z or 0-9
            */
            data -> 
                return data?.user_name ==~ /^[A-Z][0-9].*/
        }

        transform {

            /**
                Here I am adding processing date to my model
            */
            data ->
                data.process_date = LocalDateTime.now().format("yyyy-MM-dd HH:mm:ss")
                return data
        }
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "action5"

        // desired attributes required in this model
        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"

        filter {

            /**
                Here I am checking if first character of the user_name is A-Z or 0-9
            */
            data -> 
                return data?.user_name ==~ /^[A-Z][0-9].*/
        }

        transform {

            /**
                Here I am adding processing date to my model
            */
            data ->
                data.process_date = LocalDateTime.now().format("yyyy-MM-dd HH:mm:ss")
                return data
        }
    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "action6"

        // desired attributes required in this model
        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"

        filter {

            /**
                Here I am checking if first character of the user_name is A-Z or 0-9
            */
            data -> 
                return data?.user_name ==~ /^[A-Z][0-9].*/
        }

        transform {

            /**
                Here I am adding processing date to my model
            */
            data ->
                data.process_date = LocalDateTime.now().format("yyyy-MM-dd HH:mm:ss")
                return data
        }
    }


    // API model for rendering error
    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "error_api_model"

        // desired attributes required in this model
        // desired attributes required in this model
        attr "error_message" : "context._response.model.error_message"

    }


    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "get_repo_model"

        // desired attributes required in this model
        attr "repo_name" : "_response.name", "is_private" : "_response.private"
        attr "owner_name" : "_response.owner.login"
    }

    // API Model refers to the model which will render the API output
    step_model {

        // Name of the model 
        name "create_github_issue_model"

        // desired attributes required in this model
        attr "repo_name" : "_response.name", "is_private" : "_response.private"
        attr "owner_name" : "_response.owner.login"
    }
}