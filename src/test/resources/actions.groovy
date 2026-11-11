import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse
import com.freshworks.hagrid.integration.sync.test.CompareAndMatch
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE
import groovy.json.JsonSlurper


actions {

    // Case 1 Demonstration of simple get api call and return whole data as it is 

    def script = this

    action {
        name "usecase_1"
        step {
            name "github_employee"
            step_model "github_employee"
            operation "github_employee"
            response_path "context._response.body.asMap()"
        }

        output {

            consumer ->

               def cursor = consumer.getCursorByModelName("github_employee");

               def result = [:]
               def data = []
               def status = "OK"

               while(cursor.hasNext()){
                   data = cursor.getNext()
               }

               result["data"] = data
               result["status"] = status
               return result
        }
    }

    // Case 2 Demonstration of simple get api call and return whole data as it is but removing the `meta` key from target response
    action {
        name "usecase_2"
        step {
            name "github_employee_no_meta"
            step_model "github_employee_no_meta"
            operation "github_employee"
            response_path "context._response.body"
        }

        output {

            consumer ->
                return script.consume(consumer, "github_employee_no_meta", false)
        }
    }

    // Case 3 Demonstration of simple get api call and return whole data as it is but removing a particular element from the array in the target response
    action {
        name "usecase_3"
        step {
            name "github_employee_remove_element"
            step_model "github_employee_remove_element"
            operation "github_employee"
            response_path "context._response.body"
        }

        output {

                consumer ->
                    return script.consume(consumer, "github_employee_remove_element", false)
    
            }
    }

    // Case 4 Demonstration of introducing variables in the operation
    action {
        name "usecase_4"
        step {
            name "github_employee_operation_variables"
            step_model "github_employee"
            operation "github_employee_operation_variables", ["host" : "'https://api.jsonbin.io'"]
            response_path "context._response.body"
        }

        output {

                consumer ->
                    return script.consume(consumer, "github_employee", false)
    
            }
    }

    // Case 5 Demonstration of operation has variable from input
    action {
        name "usecase_5"
        step {
            name "github_employee_operation_variables_from_input"
            step_model "github_employee"
            operation "github_employee_operation_variables", ["host" : "context._input.host.asString()"]
            response_path "context._response.body"
        }

        output {

                consumer ->
                    return script.consume(consumer, "github_employee", false)
    
            }
    }

    // Case 6 Demonstration of non http operation and introduction of hooks
    action {
        name "usecase_6"
        step {
            name "compare_and_match"
            step_model "compare_and_match"
            operation "compare_and_match" , ["from_value" : "context._input.from_value.asString()", "to_value" : "context._input.to_value.asString()"]
            response_path "context._response.body"

            hooks {

                execute {

                    CompareAndMatch compareAndMatch = new CompareAndMatch()
                    def response = compareAndMatch.compare(context._operation.from_value.asString(), context._operation.to_value.asString())
                    ActionResponse actionResponse = new ActionResponse()
                    actionResponse.setBody(response)
                    actionResponse.setHttpStatus(ACTION_HTTP_CODE.OK)

                    return actionResponse

                }
            }
        }

        output {

                consumer ->
                    return script.consume(consumer, "compare_and_match", false)
    
            }
    }

    // Case 7 Demonstration of hooks and error handling
    action {
        name "usecase_7"
        step {
            name "compare_and_match"
            step_model "compare_and_match"
            operation "compare_and_match" , ["from_value" : "context._input.from_value.asString()", "to_value" : "context._input.to_value.asString()"]
            response_path "context._response.body"

            hooks {
                
                step_setup {

                    println("Perform any init tasks here")

                    // Perform any kind of init tasks here 
                    // Like fetching meta data like fields / configs from database which is not coming in input 
                    // Sanitse the input , perform logging etc 
                }

                execute {

                    CompareAndMatch compareAndMatch = new CompareAndMatch()
                    def response = compareAndMatch.compare(context._operation.from_value.asString(), context._operation.to_value.asString())
                    ActionResponse actionResponse = new ActionResponse()
                    actionResponse.setBody(response)
                    actionResponse.setHttpStatus(ACTION_HTTP_CODE.OK)
                    return actionResponse
                }

                step_parse_response {

                    // Method which helps you to handle multiple use cases 
                    // 1. Modify the response coming from third party target server
                    // 2. Assign the step_model dynamically based on the response
                        // 1. If response contains error , then change the step_model to error_handling
                        // 2. If response contains success , then change the step_model to success_handling

                    if (context._response.body.any_error == "true") {
                        println("Response contains error")
                        parse context._response.body.asMap() , "", "compare_and_match_error" 
                    }

                    else{
                        parse context._response.body.asMap() , "", "compare_and_match"
                    }
                }

                step_close{

                    println("Perform any close tasks here")

                    // Perform any kind of close tasks here 
                    // Like closing the database connection , logging etc 
                    // Saving the state like checkpoints etc 
                }
            }
        }


        output {

                consumer ->
                    return script.consume(consumer, "compare_and_match_error", false)
    
            }
    }



    action {
        name "usecase_8"
        step {
            name "pagination_action"
            step_model "open_product_api"
            operation "open_product_api"
            paginate_operation "paginated_open_product_api", ["limit" : 10, "skip" : 3]
            response_path "context._response.body.products"

            hooks {

                // Hook helps in pagination operation to determine if there are more pages to fetch
                has_more {

                    println("Calling has more hooks")
                    def returned = script.two_page_simulation()
                    println("returned is here : ${returned}")
                    return returned
                }
            }
        }
        
        output {

            consumer ->
                return script.consume(consumer, "open_product_api", true)
        }
    }


    action {
        name "usecase_9"
        step {
            name "encrich_company_information"
            step_model "encrich_company_information"
            operation "encrich_company_information", ["basic_info" : "'{\"name\" : \"amit aggarwal\"}'"]
            response_path "context._response.body.asMap()"
            is_root true

            hooks {

                execute {

                    def map = context._operation.basic_info.asMap()
                    map["company_name"] = "Freshworks"
                    println("map is here : ${map}")
                    ActionResponse actionResponse = new ActionResponse()
                    actionResponse.setBody(map)
                    actionResponse.setHttpStatus(ACTION_HTTP_CODE.OK)
                    return actionResponse
                }
            }
        }

        step {
            name "encrich_personal_information"
            step_model "encrich_personal_information"
            operation "encrich_personal_information", ["encriched_info" : "context._parent_steps[0].encriched_data.asMap()"]
            response_path "context._response.body.asMap()"
            parent "encrich_company_information"

            hooks {

                execute {
                    println("enriched info is here :");
                    println("${context._operation.encriched_info.asMap()}")
                    def map = context._operation.encriched_info.asMap()
                    println("map is here : ${map}")
                    map["personal_info"] = "Bangalore"

                    ActionResponse actionResponse = new ActionResponse()
                    actionResponse.setBody(map)
                    actionResponse.setHttpStatus(ACTION_HTTP_CODE.OK)
                    return actionResponse
                }
            }
        }

        output {

            consumer ->
                return script.consume(consumer, "encrich_personal_information", false)
        }
    }



//     action {

//         name "my_composite_action"
//         step {

//             /**
//             Use case : Very simple action with no pagination and no parameters
//             */
//             name "users"
//             step_model "step1" // idea from nango 
//             operation "step1"
//             response_path "context._response.body.body.data.users"
//             is_root true

//             hooks {

//                 step_parse_response {

//                     def how_many = context._response.body.body.how_many
//                     def users = context._response.body.body.data.users

//                     for( user in users){
//                         user["how_many"] = 3
//                     }

//                     def new_response_body = ["users" : users]
//                     parse new_response_body, "users", "step1"
//                 }
//             }
//         }


//         step {

//             /**
//             Use case : Very simple action with no pagination and no parameters
//             */
//             name "accounts"
//             step_model "step2" // idea from nango 
//             operation "step2"
//             response_path "context._response.body.body.data.users"
//             parent "step1"
//         }

//         output {

//             consumer ->

//                 def cursor = consumer.getCursorByModelName("step1", false)
//                 def result = [:]
//                 def data = [:]
//                 def status = "OK"

//                 while(cursor.hasNext()){

//                     data = cursor.getNext()
//                     break;
//                 }

//                 println("data is here : ")
//                 println("${data}")
//                 result["data"] = data
//                 result["status"] = status
//                 return result
//         }

//     } // composite action close 

} // actions close



def consume(consumer, modelName, all = false){

    def cursor = consumer.getCursorByModelName(modelName);

    def result = [:]
    def data = []
    def status = "OK"

    while(cursor.hasNext()){
        def partial = cursor.getNext()

        if(all){
            println("partial is here : ${partial}")
            data.add(partial)
        }
        else{
            data = partial
            break;
        }

    }

    result["data"] = data
    result["status"] = status
    return result
}



pagination_simulation = [true, true, false]
def two_page_simulation(){

    return pagination_simulation.pop()
}

// count = 0
// def two_times_pagination(){

//     if(count >= 2){

//         return false;
//     }

//     count = count + 1
//     return true
// }

