var exec = require('cordova/exec');

var coolMethod = function () {};

coolMethod.readCard = function (arg, success, error) {
    exec(success, error, 'UHF', 'readCard', [arg]);
}

coolMethod.searchCard = function (success, error) {
    exec(success, error, 'UHF', 'searchCard', []);
}

coolMethod.startSearchCard = function (success, error) {
    exec(success, error, 'UHF', 'startSearchCard', []);
}

coolMethod.stopSearchCard = function (success, error) {
    exec(success, error, 'UHF', 'stopSearchCard', []);
}

coolMethod.writeCard = function (arg, success, error) {
    exec(success, error, 'UHF', 'writeCard', [arg]);
}

coolMethod.getPower = function (success, error) {
    exec(success, error, 'UHF', 'getPower', []);
}

coolMethod.setPower = function (arg, success, error) {
    exec(success, error, 'UHF', 'setPower', [arg]);
}

coolMethod.getParam = function (success, error) {
    exec(success, error, 'UHF', 'getParam', []);
}

coolMethod.setParam = function (arg, success, error) {
    exec(success, error, 'UHF', 'setParam', [arg]);
}

module.exports = coolMethod;
