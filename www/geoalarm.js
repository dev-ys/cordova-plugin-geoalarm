var exec = require("cordova/exec");

module.exports = {

    initialize: function (success, error) {
        return execPromise(success, error, "GeoalarmPlugin", "initialize", []);
    },

    addAlarm: function (alarms, success, error) {
        if (!Array.isArray(alarms)) {
            alarms = [alarms];
        }

        alarms.forEach(function(alarm) {
            if (alarm.latitude) {
                alarm.latitude = coerceNumber("alarm latitude", alarm.latitude);
            } else {
                throw new Error("alarm latitude is not provided");
            }
        
            if (alarm.longitude) {
                alarm.longitude = coerceNumber("alarm longitude", alarm.longitude);
            } else {
                throw new Error("alarm longitude is not provided");
            }

            if (alarm.radius) {
                alarm.radius = coerceNumber("alarm radius", alarm.radius);
            } else {
                throw new Error("alarm radius is not provided");
            }

            if (alarm.time) {
                alarm.time = alarm.time.toString();
            } else {
                throw new Error("alarm time is not provided");
            }

            if (alarm.title) {
                alarm.title = alarm.title.toString();
            } else {
                alarm.title = "";
            }

            if (alarm.text) {
                alarm.text = alarm.text.toString();
            } else {
                alarm.text = "";
            }
        });
        return execPromise(success, error, "GeoalarmPlugin", "addAlarm", alarms);
    },

};

function execPromise(success, error, pluginName, method, args) {
    return new Promise(function (resolve, reject) {
        exec(function (result) {
                resolve(result);
                if (typeof success === "function") {
                    success(result);
                }
            },
            function (reason) {
                reject(reason);
                if (typeof error === "function") {
                    error(reason);
                }
            },
            pluginName,
            method,
            args);
    });
}

function coerceNumber(name, value) {
    if (typeof(value) !== "number") {
        console.warn(name + " is not a number, trying to convert to number");
        value = Number(value);

        if (isNaN(value)) {
            throw new Error("Cannot convert " + name + " to number");
        }
    }

    return value;
}
