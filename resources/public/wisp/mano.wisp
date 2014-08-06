(def ^:private ZnetRoyal (angular.module "ZnetRoyal" []))

(defmacro ->
    [& operations]
    (reduce
     (fn [form operation]
       (cons (first operation)
             (cons form (rest operation))))
     (first operations)
     (rest operations)))

(defn- HomeCtrl [$scope]
    (set! $scope.sometext "Type something here, you know the program is working when it's mirroring"))

(defn- RoyaltyCtrl [$scope $http]
    (set! $scope.msg "Some message to tell that the angular working properly"))

(defn- DurationCtrl [$scope $http]
    (set! $scope.msg "Some message to tell that the angular working properly"))

(defn- TopchartCtrl [$scope $http]
    (set! $scope.msg "Some message to tell that the angular working properly"))

(set! ZnetApp.controller ["HomeCtrl" "RoyaltyCtrl"])

