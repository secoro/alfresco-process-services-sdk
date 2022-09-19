/*
 * Copyright 2005-2021 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
var ALFRESCO = ALFRESCO || {};

ALFRESCO.formExtensions = {
        
        // This method is invoked when the form field have been rendered
        formRendered:function(form, scope) {
            
        },
        
        // This method is invoked when input values change (ng-change function)
        formFieldValueChanged:function(form, field, scope) {
            
        },
        
        // This method is invoked when an input field gets focus (focus event with ng-focus function)
        formFieldFocus:function(form, field, scope) {
            
        },
        
        // This method is invoked when an input field has lost focus (blur event with ng-blur function)
        formFieldBlur:function(form, field, scope) {
            
        },
        
        // This method is invoked when a person has been selected in the people picker
        formFieldPersonSelected:function(form, field, scope) {
            
        },
        
        // This method is invoked when an email has been filled-in in the people picker
        formFieldPersonEmailSelected:function(form, field, scope) {
            
        },
        
        // This method is invoked when a person has been removed in the people picker
        formFieldPersonRemoved:function(form, field, scope) {
            
        },
        
        // This method is invoked when a group has been selected in the functional group picker
        formFieldGroupSelected:function(form, field, scope) {
 
        },
        
        // This method is invoked when a group has been removed in the functional group picker
        formFieldGroupRemoved:function(form, field, scope) {
            
        },
        
        // This method is invoked when content has been uploaded in the upload field
        formFieldContentUploaded:function(form, field, scope) {
            
        },
        
        // This method is invoked when content has been removed in the upload field
        formFieldContentRemoved:function(form, field, scope) {
            
        },

        // This method is invoked when a folder has been selected in the attach folder field
        formFieldFolderSelected:function(form, field, scope) {

        },

        // This method is invoked when a folder has been removed in the attach folder field
        formFieldFolderRemoved:function(form, field, scope) {

        },

        // This method is invoked when the REST values or set in a dropdown, radio or typeahead field
        formFieldRestValuesSet:function(form, field, scope) {
        
        },
        
        // This method is invoked when the complete or an outcome button has been clicked and before the task is completed.
        formBeforeComplete:function(form, outcome, scope) {
            
        },
        
        // This method is invoked when input values change (ng-change function) in a dynamic table
        formTableFieldValueChanged:function(form, field, columnDefinition, editRow, scope) {
            
        },
        
        // This method is invoked when an input field gets focus (focus event with ng-focus function) in a dynamic table
        formTableFieldFocus:function(form, field, columnDefinition, editRow, scope) {

        },
        
        // This method is invoked when an input field has lost focus (blur event with ng-blur function) in a dynamic table
        formTableFieldBlur:function(form, field, columnDefinition, editRow, scope) {
  
        },
        
        // This method is invoked when the REST values or set in a dropdown field in a dynamic table
        formTableFieldRestValuesSet:function(form, field, columnDefinition, editRow, scope) {
            
        },
        
        // This method is invoked when the form fields have been rendered in the dynamic table popup
        formTableRendered:function(form, field, columnDefinitions, editRow, scope) {
            
        },
        
        // This method is invoked when the complete button has been clicked and before the dynamic table popup is completed.
        formTableBeforeComplete:function(form, field, editRow, scope) {
            
        },
        
        // This method is invoked when the cancel button has been clicked and before the dynamic table popup is cancelled.
        formTableBeforeCancel:function(form, field, editRow, scope) {
            
        },
        
        // This method is invoked when the form data changes. This method is invoked for every form field on the form. It will disable the complete buttons when false (boolean) is returned.
        formValidateField:function(form, field, scope) {
 
        },

        // This method is invoked when input values change (ng-change function) and will disable the complete buttons when false (boolean) is returned.
        formValidateFieldValueChanged:function(form, field, scope) {
 
        },

        // This method is invoked when the complete button has been clicked and will prevent the form completion when false (boolean) is returned.
        formValidateBeforeSubmit:function(form, outcome, scope) {
 
        },
        
        // This method is invoked when the row data changes in a dynamic table. This method is invoked for every form field in the dynamic table. It will disable the save button when false (boolean) is returned.
        formTableValidateField:function(form, field, columnDefinition, editRow, scope) {
 
        },

        // This method is invoked when input values change (ng-change function) in a dynamic table and will disable the save button when false (boolean) is returned.
        formTableValidateFieldValueChanged:function(form, field, columnDefinition, editRow, scope) {
 
        },

        // This method is invoked when the complete button has been clicked and before the dynamic table popup is completed and prevent the form completion
        // when false (boolean) is returned.
        formTableValidateBeforeComplete:function(form, field, editRow, scope) {
  
        },

        // This method is invoked when a task is completed successfully
        taskCompleted:function(taskId, form, scope) {

        },

        // This method is invoked when a task is completed unsuccessfully
        taskCompletedError:function(taskId, errorResponse, form, scope) {

        },

        // This method is invoked when a task is saved successfully
        taskSaved:function(taskId, form, scope) {

        },

        // This method is invoked when a task is saved unsuccessfully
        taskSavedError:function(taskId, errorResponse, form, scope) {

        },

        // This method is invoked when task form is saved. Prevents default save behaviour when true (boolean) is returned
        taskBeforeSaved: function (taskId, form, data, scope) {
            // return 'true' if form save is handled outside
        },

        // This method is invoked when a form tab is activated
        formTabActivated: function(form, tab, scope) {

        }
};
