import java.time.LocalDateTime

models {

    // API Model refers to the model which will render the API output
    api_model {

        // Name of the model 
        name "get_repo_model"

        // desired attributes required in this model
        attr "repo_name" : "_response.name", "is_private" : "_response.private"
        attr "owner_name" : "_response.owner.login"
    }

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "get_repo_model_output"

        // mapping of output model attributes with api model attribute
        attr "repo_name" : model('get_repo_model', 'repo_name')
        attr "is_private" : model('get_repo_model', 'is_private')
        attr "owner_name" : model('get_repo_model', 'owner_name')
    }

    
    // API Model refers to the model which will render the API output
    api_model {

        // Name of the model 
        name "action1"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"

        transform
    }

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "action1_output"

        // mapping of output model attributes with api model attribute
        attr "user_name_formatted" : model('action1', 'user_name') , "user_id_formatted" : model('action1' , 'user_id')
    }

    // API Model refers to the model which will render the API output
    api_model {

        // Name of the model 
        name "action2"

        // desired attributes required in this model
        attr "user_id" : "context._response.model.user_id", "user_name" : "context._response.model.user_name"
    }

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "action2_output"

        // mapping of output model attributes with api model attribute
        attr "user_name" : model('action2', 'user_name') , "user_id" : model('action2' , 'user_id')
    }


    // API Model refers to the model which will render the API output
    api_model {

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

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "action3_output"

        // mapping of output model attributes with api model attribute
        attr "user_name" : model('action3', 'user_name') , "user_id" : model('action3' , 'user_id')
    }

    // API Model refers to the model which will render the API output
    api_model {

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

    // Output model refers to the model which can be filled with other API model or output model to create desired business object
    output_model {

        // Name of the output model
        name "action4_output"

        // mapping of output model attributes with api model attribute
        attr "user_name" : model('action4', 'user_name') , "user_id" : model('action4' , 'user_id')
        attr "process_date" : model('action4', 'process_date')
    }

    // API Model refers to the model which will render the API output
    api_model {

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

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "action5_output"

        // mapping of output model attributes with api model attribute
        attr "user_name" : model('action5', 'user_name') , "user_id" : model('action5' , 'user_id')
        attr "process_date" : model('action5', 'process_date')
    }

        // API Model refers to the model which will render the API output
    api_model {

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

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "action6_output"

        // mapping of output model attributes with api model attribute
        attr "user_name" : model('action6', 'user_name') , "user_id" : model('action6' , 'user_id')
        attr "process_date" : model('action6', 'process_date')
    }


    // API model for rendering error

            // API Model refers to the model which will render the API output
    api_model {

        // Name of the model 
        name "error_api_model"

        // desired attributes required in this model
        // desired attributes required in this model
        attr "error_message" : "context._response.model.error_message"

    }

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "error_output_model"

        // mapping of output model attributes with api model attribute
        attr "error_message" : model('error_api_model', 'error_message')
    }


    // API Model refers to the model which will render the API output
    api_model {

        // Name of the model 
        name "get_repo_model"

        // desired attributes required in this model
        attr "repo_name" : "_response.name", "is_private" : "_response.private"
        attr "owner_name" : "_response.owner.login"
    }

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "get_repo_model_output"

        // mapping of output model attributes with api model attribute
        attr "repo_name" : model('get_repo_model', 'repo_name')
        attr "is_private" : model('get_repo_model', 'is_private')
        attr "owner_name" : model('get_repo_model', 'owner_name')
    }

        // API Model refers to the model which will render the API output
    api_model {

        // Name of the model 
        name "create_github_issue_model"

        // desired attributes required in this model
        attr "repo_name" : "_response.name", "is_private" : "_response.private"
        attr "owner_name" : "_response.owner.login"
    }

    // Output model refers to the model which can be filled with other API model to create desired business object
    output_model {

        // Name of the output model
        name "create_github_issue_model_output"

        // mapping of output model attributes with api model attribute
        attr "repo_name" : model('get_repo_model', 'repo_name')
        attr "is_private" : model('get_repo_model', 'is_private')
        attr "owner_name" : model('get_repo_model', 'owner_name')
    }
}