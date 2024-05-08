/*!
 * mars
 * Copyright(c) 2015 huangbinglong
 * MIT Licensed
 */

angular.module("ui.neptune.directive.upload", [])
    .controller("UploadControllect", function ($scope, $http, $q) {
        var vm = this;
        vm.options = {
            showUploadBtn:true
        };
        vm.options = angular.extend(vm.options,$scope.nptUpload || {});

        vm.filesInfo = [];
        vm.errors = [];
        // 开始上传文件
        vm.startUpload = function () {
            set_upload_param(uploader).then(function () {
                uploader.start();
            }, function (err) {
                console.error(err);
            });
        };

        // 删除指定文件
        vm.removeFile = function (file) {
            uploader.removeFile(file);
        };

        var expire = 0;
        var dir = "";
        // 从服务器获取签名信息
        function get_signature() {
            //可以判断当前expire是否超过了当前时间,如果超过了当前时间,就重新取一下.3s 做为缓冲
            var now = Date.parse(new Date()) / 1000;
            console.log('get_signature ...');
            console.log('expire:' + expire.toString());
            console.log('now:', +now.toString());
            if (expire < now + 3 && vm.options.getSignature) {
                return vm.options.getSignature();
            }
            return false;
        }

        function set_upload_param(up) {
            var deffer = $q.defer();
            var ret = get_signature();//通过服务器获取上传配置
            var new_multipart_params = {};
            new_multipart_params.key = guid();
            if (ret) {
                ret.then(function (response) {
                    var data = response.data;
                    expire = parseInt(data.expire);
                    dir = data.dir;
                    new_multipart_params.Filename = "${filename}";
                    new_multipart_params.key = dir + new_multipart_params.key;
                    new_multipart_params.policy = data.policy;
                    new_multipart_params.OSSAccessKeyId = data.accessid;
                    new_multipart_params.success_action_status = '200';//让服务端返回200,不然，默认会返回204
                    new_multipart_params.signature = data.signature;

                    up.setOption({
                        'url': data.host,
                        'multipart_params': new_multipart_params
                    });
                    deffer.resolve(data);
                }, function (err) {
                    deffer.reject(err);
                });
            } else {
                up.getOption().multipart_params.key = dir + new_multipart_params.key;
                deffer.resolve();
            }
            return deffer.promise;
        }

        function guid() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                return v.toString(16);
            });
        }

        var uploadOptions = {
            runtimes: 'html5,flash,silverlight,html4',
            browse_button: 'selectfiles',
            container: document.getElementById('container'),
            flash_swf_url: '/vendor/plupload-2.1.2/js/Moxie.swf',
            silverlight_xap_url: '/vendor/plupload-2.1.2/js/Moxie.xap',
            url:'http://oss-cn-shenzhen.aliyuncs.com',
            prevent_duplicates:true,
            multi_selection:true,

            init: {
                PostInit: function () {
                },

                FilesAdded: function (up, files) {
                    plupload.each(files, function (file) {
                        file.formateSize = plupload.formatSize(file.size);
                        vm.filesInfo.push(file);
                    });
                    if (vm.options.filesAdded) {
                        vm.options.filesAdded(files);
                    }
                    $scope.$apply();
                },

                UploadProgress: function (up, file) {
                    $scope.$apply();
                    if (vm.options.uploadProgress) {
                        vm.options.uploadProgress(file);
                    }
                },

                FileUploaded: function (up, file, info) {
                    if (info.status >= 200 || info.status < 200) {
                        var key = up.getOption().multipart_params.key;
                        if (key.indexOf("/") > 0) {
                            var path = key.split("/");
                            key = path[path.length - 1];
                        }
                        file.UUID = key;
                        file.uploadState = "成功";
                        if (vm.options.fileUploaded) {
                            vm.options.fileUploaded(file,info);
                        }
                    }
                    else {
                        file.uploadState = info.response;
                    }
                    set_upload_param(up);
                    $scope.$apply();
                },
                Browse:function(up,file) {
                    vm.errors = [];
                },
                UploadComplete : function(up,files) {
                    if (vm.options.uploadComplete) {
                        vm.options.uploadComplete(files);
                    }
                },
                FilesRemoved:function(up,files) {
                    vm.filesInfo = [];
                    up.files.forEach(function(f) {
                        vm.filesInfo.push(f);
                    });
                    if (vm.options.filesRemoved) {
                        vm.options.filesRemoved(files);
                    }
                },
                Error: function (up, err) {
                    set_upload_param(up);
                    vm.errors.push(err);
                    $scope.$apply();
                }
            }
        };
        uploadOptions = angular.extend(uploadOptions,vm.options.up || {});
        var uploader = new plupload.Uploader(uploadOptions);
        uploader.init();

        uploader.startUpload = vm.startUpload;

        if (vm.options.onRegisterApi) {
            vm.options.onRegisterApi({
                uploader:uploader
            });
        }

    })
    .directive("nptUpload", [function () {
        return {
            restrict: "EA",
            controller: "UploadControllect as vm",
            replace: true,
            templateUrl: function (element, attrs) {
                return attrs.templateUrl || "/template/upload/upload.html";
            },
            scope: {
                nptUpload: "="
            },
            link: function () {
            }
        };
    }]);
