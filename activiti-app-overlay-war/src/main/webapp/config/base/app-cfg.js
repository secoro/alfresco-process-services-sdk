'use strict';

var ACTIVITI = ACTIVITI || {};

ACTIVITI.CONFIG = {
	'onPremise' : true,
	'contextRoot' : '/activiti-app',
	'webContextRoot' : '/activiti-app',
    'locales': ['en','de','es','fr','it','nl','ja','nb','ru','zh-CN','pt-BR'],
    'signupUrl' : '#/signup'
};

ACTIVITI.CONFIG.resources = {
    '*': [
        {
            'tag': 'link',
            'rel': 'stylesheet',
            'href': ACTIVITI.CONFIG.webContextRoot + '/styles/custom.css?v=20200702'
        }
    ]
};