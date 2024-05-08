"use strict";

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

var _get = function get(_x, _x2, _x3) { var _again = true; _function: while (_again) { var object = _x, property = _x2, receiver = _x3; desc = parent = getter = undefined; _again = false; if (object === null) object = Function.prototype; var desc = Object.getOwnPropertyDescriptor(object, property); if (desc === undefined) { var parent = Object.getPrototypeOf(object); if (parent === null) { return undefined; } else { _x = parent; _x2 = property; _x3 = receiver; _again = true; continue _function; } } else if ("value" in desc) { return desc.value; } else { var getter = desc.get; if (getter === undefined) { return undefined; } return getter.call(receiver); } } };

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) subClass.__proto__ = superClass; }

var _libRequestJs = require("../lib/request.js");

var _libRequestJs2 = _interopRequireDefault(_libRequestJs);

var _omnirouter = require("omnirouter");

var _omnirouter2 = _interopRequireDefault(_omnirouter);

var _libResponseJs = require("../lib/response.js");

var _libResponseJs2 = _interopRequireDefault(_libResponseJs);

var sinon = require("sinon");

describe("Request", function () {

	describe("(static properties)", function () {
		describe(".post", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"].post.should.be.instanceOf(_libRequestJs2["default"]);
			});
		});

		describe(".put", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"].put.should.be.instanceOf(_libRequestJs2["default"]);
			});
		});

		describe(".get", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"].get.should.be.instanceOf(_libRequestJs2["default"]);
			});
		});

		describe(".delete", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"]["delete"].should.be.instanceOf(_libRequestJs2["default"]);
			});
		});
	});

	describe("(instance methods)", function () {
		describe(".url", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"].post.url("http://www.test.com").should.be.instanceOf(_libRequestJs2["default"]);
			});
		});

		describe(".data", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"].post.data("http://www.test.com").should.be.instanceOf(_libRequestJs2["default"]);
			});
		});

		describe(".header", function () {
			it("should return a request object instance", function () {
				_libRequestJs2["default"].post.header("http://www.test.com").should.be.instanceOf(_libRequestJs2["default"]);
			});
		});

		describe(".results", function () {
			it("should callback", function (done) {
				_libRequestJs2["default"].post.results(function () {
					done();
				});
			});
		});
	});

	describe("(functionality)", function () {
		var requestData = { somenumber: 23, somestring: "somestring" },
		    responseData = { somenumber: 344, somestring: "responsestring" };

		var postSpy = undefined,
		    putSpy = undefined,
		    getSpy = undefined,
		    deleteSpy = undefined,
		    urlPost = "/test",
		    urlPut = "/test/1",
		    urlGet = "/test/1",
		    urlDelete = "/test/1",
		    router = undefined,
		    portNumber = 1339,
		    headers = undefined,
		    headersSpy = undefined,
		    requestBodySpy = undefined,
		    responseBodySpy = undefined,
		    completeUrl = undefined;

		var TestRouter = (function (_Router) {
			function TestRouter() {
				_classCallCheck(this, TestRouter);

				_get(Object.getPrototypeOf(TestRouter.prototype), "constructor", this).apply(this, arguments);
			}

			_inherits(TestRouter, _Router);

			_createClass(TestRouter, [{
				key: "initialize",
				value: function initialize() {
					this.post(urlPost, function (request, response) {
						postSpy(request, response);
					});
					this.put(urlPut, function (request, response) {
						putSpy(request, response);
					});
					this.get(urlGet, function (request, response) {
						getSpy(request, response);
					});
					this["delete"](urlDelete, function (request, response) {
						deleteSpy(request, response);
					});
				}
			}]);

			return TestRouter;
		})(_omnirouter2["default"]);

		before(function (done) {
			headers = {
				"Content-Type": "application/vnd.api+json",
				"Custom-Header": "customvalue"
			};
			router = new TestRouter();
			router.listen(portNumber, function () {
				done();
			});
		});

		after(function (done) {
			router.close(function () {
				done();
			});
		});

		beforeEach(function () {
			headersSpy = sinon.spy();
			responseBodySpy = sinon.spy();
			requestBodySpy = sinon.spy();
		});

		describe("(complete chains)", function () {
			describe("Request.post.url.data.header.results", function () {
				beforeEach(function (done) {
					completeUrl = "http://localhost:" + portNumber + urlPost;

					postSpy = sinon.spy(function (request, response) {
						var receivedHeaders = {
							"Content-Type": request.header("Content-Type"),
							"Custom-Header": request.header("Custom-Header")
						};
						headersSpy(receivedHeaders);
						requestBodySpy(request.body);
						response.send(responseData);
					});

					_libRequestJs2["default"].post.url(completeUrl).data(requestData).header("Content-Type", headers["Content-Type"]).header("Custom-Header", headers["Custom-Header"]).results(function (error, result) {
						if (result) {
							responseBodySpy(result.body);
						}
						done();
					});
				});

				it("should send the appropiate headers", function () {
					headersSpy.calledWithExactly(headers).should.be["true"];
				});

				it("should use the appropiate url + method", function () {
					postSpy.called.should.be["true"];
				});

				it("should get the appropiate result", function () {
					responseBodySpy.calledWithExactly(responseData).should.be["true"];
				});

				it("should get the appropiate result", function () {
					responseBodySpy.calledWithExactly(responseData).should.be["true"];
				});

				it("should send the appropiate body", function () {
					requestBodySpy.calledWithExactly(requestData).should.be["true"];
				});
			});

			describe("Request.put.url.data.header.results", function () {
				beforeEach(function (done) {
					completeUrl = "http://localhost:" + portNumber + urlPut;

					putSpy = sinon.spy(function (request, response) {
						var receivedHeaders = {
							"Content-Type": request.header("Content-Type"),
							"Custom-Header": request.header("Custom-Header")
						};
						headersSpy(receivedHeaders);
						requestBodySpy(request.body);
						response.send(responseData);
					});

					_libRequestJs2["default"].put.url(completeUrl).data(requestData).header("Content-Type", headers["Content-Type"]).header("Custom-Header", headers["Custom-Header"]).results(function (error, result) {
						if (result) {
							responseBodySpy(result.body);
						}
						done();
					});
				});

				it("should send the appropiate headers", function () {
					headersSpy.calledWithExactly(headers).should.be["true"];
				});

				it("should use the appropiate url + method", function () {
					putSpy.called.should.be["true"];
				});

				it("should get the appropiate result", function () {
					responseBodySpy.calledWithExactly(responseData).should.be["true"];
				});

				it("should send the appropiate body", function () {
					requestBodySpy.calledWithExactly(requestData).should.be["true"];
				});
			});

			describe("Request.get.url.header.results", function () {
				beforeEach(function (done) {
					completeUrl = "http://localhost:" + portNumber + urlGet;

					getSpy = sinon.spy(function (request, response) {
						var receivedHeaders = {
							"Content-Type": request.header("Content-Type"),
							"Custom-Header": request.header("Custom-Header")
						};
						headersSpy(receivedHeaders);
						response.send(responseData);
					});

					_libRequestJs2["default"].get.url(completeUrl).header("Content-Type", headers["Content-Type"]).header("Custom-Header", headers["Custom-Header"]).results(function (error, result) {
						if (result) {
							responseBodySpy(result.body);
						}
						done();
					});
				});

				it("should send the appropiate headers", function () {
					headersSpy.calledWithExactly(headers).should.be["true"];
				});

				it("should use the appropiate url + method", function () {
					getSpy.called.should.be["true"];
				});

				it("should get the appropiate result", function () {
					responseBodySpy.calledWithExactly(responseData).should.be["true"];
				});
			});

			describe("Request.delete.url.header.results", function () {
				beforeEach(function (done) {
					completeUrl = "http://localhost:" + portNumber + urlDelete;

					deleteSpy = sinon.spy(function (request, response) {
						var receivedHeaders = {
							"Content-Type": request.header("Content-Type"),
							"Custom-Header": request.header("Custom-Header")
						};
						headersSpy(receivedHeaders);
						response.send(responseData);
					});

					_libRequestJs2["default"]["delete"].url(completeUrl).header("Content-Type", headers["Content-Type"]).header("Custom-Header", headers["Custom-Header"]).results(function (error, result) {
						if (result) {
							responseBodySpy(result.body);
						}
						done();
					});
				});

				it("should send the appropiate headers", function () {
					headersSpy.calledWithExactly(headers).should.be["true"];
				});

				it("should use the appropiate url + method", function () {
					deleteSpy.called.should.be["true"];
				});

				it("should get the appropiate result", function () {
					responseBodySpy.calledWithExactly(responseData).should.be["true"];
				});
			});
		});

		describe("(callback response)", function () {
			it("it should be an instance of the Response object", function (done) {
				_libRequestJs2["default"].get.url(completeUrl).header("Content-Type", headers["Content-Type"]).header("Custom-Header", headers["Custom-Header"]).results(function (error, result) {
					result.should.be.instanceOf(_libResponseJs2["default"]);
					done();
				});
			});

			it("it should return json with a custom json content type", function (done) {
				getSpy = sinon.spy(function (request, response) {
					response.set("Content-Type", "application/vnd.api+json");
					response.send(responseData);
				});

				_libRequestJs2["default"].get.url(completeUrl).header("Content-Type", headers["Content-Type"]).header("Custom-Header", headers["Custom-Header"]).results(function (error, result) {
					result.body.should.eql(responseData);
					done();
				});
			});
		});
	});
});