/*
 * Copyright 2005-2021 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
(function(resources){
    if (!resources) {
        resources = {};
    }

    function load(res, node, callback, scope) {
        var resource;
        if (res.tag === 'script') {
            resource = document.createElement('script');
            resource.type = res.type || 'text/javascript';
            resource.src = res.src;

            if (callback) {
                var done = false;

                // Attach handlers for all browsers
                resource.onload = resource.onreadystatechange = function()
                {
                    if (!done && (!this.readyState || this.readyState == 'loaded' || this.readyState == 'complete'))
                    {
                        done = true;
                        callback.call(scope ? scope : this, res);
                    }
                };
            }
        }
        else if (res.tag === 'link') {
            resource = document.createElement('link');
            resource.rel = res.rel || 'stylesheet';
            resource.href = res.href;
        }

        if (node.nextSibling) {
            node.parentNode.insertBefore(resource, node.nextSibling);
        }
        else {
            node.parentNode.appendChild(resource);
        }

        if (res.tag === 'link' && callback) {
            callback.call(scope ? scope : this, res);
        }
    }

    function getResourceLoaderElement() {
        var scripts = document.getElementsByTagName('script');
        for (var i = 0, il = scripts.length; i < il; i++) {
            if (scripts[i].src.indexOf('scripts/resource-loader.js') != -1) {
                return scripts[i];
            }
        }
        return null;
    }

    var res = resources['*'];
    var resourceLoaderElement = getResourceLoaderElement();
    var appName = resourceLoaderElement.getAttribute('app');
    var angularModuleName = resourceLoaderElement.getAttribute('module');
    if (resources.hasOwnProperty(appName)) {
        res = resources[appName];
    }
    if (!res) {
        res = [];
    }

    // Always load the locale to use from the server so its determined from the Accept-Language header
    // since IE doesn't reflect the user's language choice in javascript correctly and also to
    // let us decide the preferred language rather than using ng-translate's logic which only honours the first user
    // defined language.
    var locales = ACTIVITI.CONFIG.locales || ['en','de','es','fr','it','nl','ja','nb','ru','zh-CN','pt-BR']; // Note! Make sure this matches app-cfg.js
    res.push({
        tag: 'script',
        type: 'text/javascript',
        src: ACTIVITI.CONFIG.contextRoot + '/app/rest/locale?available=' + locales.join(',') + '&cache=' + new Date().getTime()
    });

    var loadedResources = 0;
    for (var i = 0, il = res.length; i < il; i++) {
        load(res[i], resourceLoaderElement, function(){
            loadedResources++;
            if (loadedResources == res.length) {
                // Let angular resume bootstrap
                var interval = window.setInterval(function(){
                    if (angular && typeof angular.bootstrap == 'function') {
                        angular.element(document.body).ready(function() {
                            // angular, all of our resources & the document body are ready, lets bootstrap the app
                            angular.bootstrap(document.body, [angularModuleName]);
                        });
                        window.clearInterval(interval);
                    }
                }, 20);
            }
        });
    }
})(ACTIVITI.CONFIG.resources);