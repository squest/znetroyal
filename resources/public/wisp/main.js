var znetApp = angular.module("znetApp", []);

var AnimDuration = 3000;

function animateNotes (divID, expectedDuration) {
	// body...
	var zcon = document.getElementById(divID);
	Jacked.fadeIn(zcon, {duration: expectedDuration});
}

var CategoriesCtrl = function CategoriesCtrl($scope, $http)
{
	/// initialising data
	
	$http.get('/catdata?requestedData=links')
	.success(function (data){
		$scope.linksLevel1 = data.linksLevel1;
		$scope.linksLevel2 = data.linksLevel2;

	});

	$http.get('/catdata?requestedData=initReport')
	.success(function (data) {
		$scope.webdata = data;
		animateNotes('znContent', AnimDuration);
	});
	
	/// functions to trigger data fetching from server and replace the model with data from server

	$scope.getReportLevel1 = function (number){
		switch (number) {
			case 0: timeframe = ":historic";
					break;
			case 1: timeframe = ":janfeb14";
					break;
			default: timeframe = ":historic";
		};

		$http.get("/reportdata?level=1&timeframe=" + timeframe)
		.success(function (data) {
			$scope.webdata = data;
			animateNotes('znContent', AnimDuration);
		});

	};

	$scope.getReportLevel2 = function (number){

		switch (number) {
			case 0: timeframe = ":historic";
					break;
			case 1: timeframe = ":janfeb14";
					break;
			default: timeframe = ":historic";
		};
		
		$http.get("/reportdata?level=2&timeframe=" + timeframe)
		.success(function (data) {
			$scope.webdata = data;
			animateNotes('znContent', AnimDuration);
		});

		
	};
	
};

znetApp.controller = ["CategoriesCtrl"];