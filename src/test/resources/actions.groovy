actions {

    // simple_action {

    //     /**
    //       Use case : Very simple action with no pagination and no parameters
    //     */
    //     name "getUser"
    //     api_model "action1" // idea from nango 
    //     request "action1"
    //     response_path "context._response.body.body.data.users"
    //     output "action1_output"
    // }

    //  simple_action {

    //     /**
    //       Use case : Github action with request template params replacement 
    //     */
    //     name "create_github_issue"
    //     api_model "create_github_issue_model" 
    //     request "create_github_issue_request", ["my_token": "_input.github_token", "owner" : "_input.github_owner", "repo" : "_input.github_repo" ,"issue_title" : "_input.github_issue_title", "issue_body" : "_input.github_issue_body"]
    //     response_path "context._response"
    //     output "create_github_issue_model_output"
    // }


    // simple_action {

    //     /**
    //       Use case : Github action with request template params replacement
    //     */
    //     name "get_github_repo"
    //     api_model "get_repo_model" 
    //     request "get_repo_request", ["per_page" : "_input.how_many.count", "my_token": "_input.github_token"]
    //     response_path "context._response"
    //     output "get_repo_model_output"
    // }


    // simple_action {

    //     /**
    //       Use case : Simple action with input as parameter
    //     */
    //     name "action2"
    //     api_model "action2" // idea from nango 
    //     request "action2", ["how_many" : "context._input.how_many.count"]
    //     response_path "context._response.body.body.data.users"
    //     output "action2_output"
    // }

    // simple_action {

    //     /**
    //       Use case : Simple action with input as parameter and filter data .. if user_name starts with captial A-Z 
    //        or 0-9 then consider it other wise drop it 
    //     */
    //     name "action3"
    //     api_model "action3" // idea from nango 
    //     request "action3", ["how_many" : "context._input.how_many.count"]
    //     response_path "context._response.body.body.data.users"
    //     output "action3_output"
    // }

    // simple_action {

    //     /**
    //       Use case : Simple action with input as parameter, transform data and filter data .. 
    //         in transform data, add a processing date column along with it.
    //     */
    //     name "action4"
    //     api_model "action4" // idea from nango 
    //     request "action4", ["how_many" : "context._input.how_many.count"]
    //     response_path "context._response.body.body.data.users"
    //     output "action4_output"
    // }

    // simple_action {

    //     /**
    //       Use case : Use case 4 + case of pagination where pagination is coming from response itself
    //     */
    //     name "action5"
    //     api_model "action5" // idea from nango 
    //     request "action5", ["how_many" : "context._input.how_many.count"]
    //     paginate_request "action5", ["how_many" : "context._response.body.how_many"]
    //     response_path "context._response.body.body.data.users"
    //     output "action5_output"
    // }


    // simple_action {

    //     /**
    //       Use case : Use case 5 + case when developer want to do some set up methods like calling api, logging etc
    //     */
    //     name "action6"
    //     api_model "action6" // idea from nango 
    //     request "action6", ["how_many" : "context._input.how_many.count"]
    //     paginate_request "action6", ["how_many" : "context._response.body.how_many"]
    //     response_path "context._response.body.body.data.users"
    //     output "action6_output"
    //     hooks {

    //         // When - Called once after `should_skip_this_parent` return false, 
    //         // Usecase - Perform any kind of setup like logging, input validation, input transformation, api call to fetch extra information
    //         // Return - Nothing 
    //         // default - void
    //         // Optional - Yes
    //         action_setup {
    //             println "action set up is called"
    //         }
    //     }
    // }


    // simple_action {

    //     /**
    //       Use case : Use case 6 + custom method decide whether hagrid should go through next page or not
    //     */
    //     name "action7"
    //     api_model "action7" // idea from nango 
    //     request "action7", ["how_many" : "context._input.how_many.count"]
    //     paginate_request "action7", ["how_many" : "context._response.body.how_many"]
    //     response_path "context._response.body.body.data.users"
    //     output "action7_output"
    //     hooks {

    //         // When - Called after action_parse_response is called.
    //         // Usecase - Decide whether more pages exists for this action or not 
    //         // Return - true, if more pages are present. if true then paginate_request will be used further to make the call
    //         // Return - false, if no more pages are present
    //         // default - false
    //         // Optional - Yes
    //         has_more {

    //             println "has more is called"
    //             return hasMoreMethod()
    //         }
    //     }
    // }


    // simple_action {

    //     /**
    //       Use case : Use case 7 + case of error where developer want to dynamically change the API model and output model based on the response returned by the target server
    //     */
    //     name "action8"
    //     api_model "action8" // idea from nango 
    //     request "action8", ["how_many" : "context._input.how_many.count"]
    //     paginate_request "action8", ["how_many" : "context._response.body.how_many"]
    //     response_path "context._response.body.body.data.users"
    //     output "action8_output"
    //     hooks {

    //         // When - Called after should_passthrough_response is true
    //         // Usecase - Based on the response, decide in which api_model and output_model, it should be rendered. 
    //         // Usecase - By default, Hagrid will render response in api_model mentioned with action. However in some cases (like in error response cases), you may want to render it in different model 
    //         // Return - Return 3 items, response_path, api_model, output_model
    //         // default - By default, Hagrid will try to render it into api_model and output_model mentioned in the action definitation
    //         // Optional - Yes
    //         action_parse_response {

    //             println "parse response is called " + context._response.body
    //             parse "body.data.users", "error_api_model", "error_output_model", "BAD"
    //         }
    //     }
    // }


    // simple_action {

    //     /**
    //       Use case : Use case 8 + all hooks available to the developer to customise any part of the connector life cycle
    //     */
    //     name "action9"
    //     api_model "action9" // idea from nango 
    //     request "action9", ["how_many" : "context._input.how_many.count", "has_error" : "context._input.has_errors"]
    //     paginate_request "action9", ["how_many" : "context._response.body.how_many"]
    //     response_path "context._response.body.body.data.users"
    //     output "action9_output"
    //     hooks {

    //         // When - First method to be called when action is started
    //         // Usecase - Useful in case of hierarichal actions. For simple actions, always false
    //         // Return - Nothing 
    //         // default - true 
    //         // Optional - Yes
    //         should_skip_this_parent {
    //             println "should skip this parent is called"
    //             return false
    //         }


    //         // When - Called once after `should_skip_this_parent` return false, 
    //         // Usecase - Perform any kind of setup like logging etc 
    //         // Return - Nothing 
    //         // default - void
    //         // Optional - Yes
    //         action_setup {
    //             println "action set up is called"
    //         }

    //         // When - Called when Hagrid fetches the response from third party 
    //         // Usecase - Decide whether you want to consume this response or perform re-try, abort or holdAndRetry kind of actions
    //         // Return - true, when you want this response to be captured and you want to get this response irrespective of whether it is error response or success response
    //         // return false, when you want to perform actions like `re-Try`, `abort`, `wait` etc 
    //         // default - true if response code is 2XX and false if it is NOT 2XX 
    //         // Optional - Yes
    //         should_passthrough_response {

    //             println "is valid response is called"
    //             return true 
    //         }

    //         // When - Called after should_passthrough_response is true
    //         // Usecase - Based on the response, decide in which api_model and output_model, it should be rendered. 
    //         // Usecase - By default, Hagrid will render response in api_model mentioned with action. However in some cases (like in error response cases), you may want to render it in different model 
    //         // Return - Return 3 items, response_path, api_model, output_model
    //         // default - By default, Hagrid will try to render it into api_model and output_model mentioned in the action definitation
    //         // Optional - Yes
    //         action_parse_response {

    //             println "parse response is called " + context._response.body
    //             parse "body.data.users", "error_api_model", "error_output_model", "BAD"
    //         }

    //         // When - Called after action_parse_response is called.
    //         // Usecase - Decide whether more pages exists for this action or not 
    //         // Return - true, if more pages are present. if true then paginate_request will be used further to make the call
    //         // Return - false, if no more pages are present
    //         // default - false
    //         // Optional - Yes
    //         has_more {

    //             println "has more is called"
    //             return false
    //         }

    //         // When - Called in the last 
    //         // Usecase - Perform tear down tasks.. 
    //         // Return - void
    //         // default - void
    //         // Optional - Yes
    //         action_close {
                    
    //             println "action close is called"
    //         }
    //     }
    // }


    // composite_action {

    //     name "sync_azure_ad_app_user_usages"

    //     simple_action {

    //         /**
    //         Use case : Fetch all applications from azureAD
    //         */
    //         name "get_azure_application"
    //         api_model "azure_application" // idea from nango 
    //         request "get_azure_application_request"
    //         response_path "context._response.body.body.data.apps"
    //         output "azure_application_output"
    //         is_root true

    //     }

    //     simple_action {

    //         /**
    //         Use case : Fetch all users for a given application
    //         */
    //         name "get_azure_app_users"
    //         api_model "azure_application_users" // idea from nango 
    //         request "get_azure_app_users_request"
    //         response_path "context._response.body.body.data.users"
    //         output "azure_app_users_output"
    //         parent "getAzureApplications"
    //     }

    //     simple_action {

    //         /**
    //         Use case : Fetch all usages for a given application
    //         */
    //         name "get_azure_app_usages"
    //         api_model "azure_" // idea from nango 
    //         request "get_azure_app_usages_request"
    //         response_path "context._response.body.body.data.usages"
    //         output "azure_app_usages_output"
    //         parent "getAzureApplications"
    //     }
    // }


    composite_action {

        name "json_processing"
        
        simple_action {

            /**
            Use case : Very simple action with no pagination and no parameters
            */
            name "getUser"
            api_model "action1" // idea from nango 
            request "action1"
            response_path "context._response.body.body.data.users"
            output "action1_output"
            is_root true
        }

        simple_action {

            /**
            Use case : Very simple action with no pagination and no parameters
            */
            name "getUser"
            api_model "action1" // idea from nango 
            request "action1"
            response_path "context._response.body.body.data.users"
            output "action1_output"
            parent "getUser"
        }

    }
}






pagination_simulation = [true, true, false]
def two_page_simulation(){

    return pagination_simulation.pop()
}

count = 0
def two_times_pagination(){

    if(count >= 2){

        return false;
    }

    count = count + 1
    return true
}







