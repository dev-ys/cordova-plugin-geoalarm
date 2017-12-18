//
//  GeoalarmPlugin.swift
//  ionic-geoalarm
//
//
//
import Foundation
import WebKit
import UserNotifications

let TAG = "GeoalarmPlugin"
let iOS8 = floor(NSFoundationVersionNumber) > floor(NSFoundationVersionNumber_iOS_7_1)
let iOS7 = floor(NSFoundationVersionNumber) <= floor(NSFoundationVersionNumber_iOS_7_1)

let REGIONS = "geoalarm_regions"
let REGION_SEPARATOR = "#"
let TIME_SEPARATOR = "#"

@available(iOS 10.0, *)
@objc(GeoalarmPlugin) class GeoalarmPlugin : CDVPlugin {
    lazy var geoLocationManager = GeoLocationManager()
    let priority = DispatchQoS.QoSClass.default
    
    override func pluginInitialize () {
        
    }
    
    func initialize(_ command: CDVInvokedUrlCommand) {
        print("Plugin initialization")
        
        geoLocationManager = GeoLocationManager()
        let (ok, warnings, errors) = geoLocationManager.checkRequirements()
        
        print(warnings)
        print(errors)
        
        let result: CDVPluginResult
        
        if ok {
            result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: warnings.joined(separator: "\n"))
        } else {
            result = CDVPluginResult(
                status: CDVCommandStatus_ILLEGAL_ACCESS_EXCEPTION,
                messageAs: (errors + warnings).joined(separator: "\n")
            )
        }
        
        commandDelegate!.send(result, callbackId: command.callbackId)
    }
    
    func addAlarm(_ command: CDVInvokedUrlCommand) {
        DispatchQueue.global(qos: DispatchQoS.QoSClass.default).async {
            for geo in command.arguments {
                self.geoLocationManager.addAlarm(JSON(geo))
            }
            DispatchQueue.main.async {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            }
        }
    }
    
}

@available(iOS 10.0, *)
class GeoLocationManager : NSObject, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    let notificationManager = NotificationManager()
    let location: CLLocation! = nil
    
    override init() {
        print("GeoLocationManager init")
        super.init()
        
        locationManager.requestAlwaysAuthorization()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
//        locationManager.startUpdatingLocation()
        
        // remove all locations and alarms
        
        if let regionIds = getRegionIds() {
            for region in locationManager.monitoredRegions {
                if regionIds.contains(region.identifier) {
                    locationManager.stopMonitoring(for: region)
                    notificationManager.removeAlarm(regionId: region.identifier)
                }
            }
            UserDefaults.standard.removeObject(forKey: REGIONS)
            UserDefaults.standard.synchronize()
        }
    }
    
    func getRegionIds() -> [String]? {
        let str = UserDefaults.standard.string(forKey: REGIONS)
        return str?.components(separatedBy: REGION_SEPARATOR)
    }
    
    func addAlarm(_ info: JSON) {
        let latitude = info["latitude"].doubleValue
        let longitude = info["longitude"].doubleValue
        let radius = info["radius"].doubleValue as CLLocationDistance
        let regionId = "\(latitude)_\(longitude)_\(radius)"
        startMonitoringForRegion(regionId: regionId, latitude: latitude, longitude: longitude, radius: radius)
        
        let time = info["time"].stringValue
        let title = info["title"].stringValue
        let text = info["text"].stringValue
        notificationManager.addAlarm(regionId: regionId, time: time, title: title, text: text)
        
        let center = CLLocation(latitude: latitude, longitude: longitude)
        if locationManager.location != nil {
            let distance = center.distance(from: locationManager.location!)
            if distance < radius {
                notificationManager.enableAlarm(regionId: regionId, time: time)
            }
        }
    }
    
    func startMonitoringForRegion(regionId: String, latitude: Double, longitude: Double, radius: CLLocationDistance) {
        
        var regionIds = getRegionIds()
        if regionIds?.contains(regionId) == true {
            return
        }
        
        // start
        
        let center = CLLocationCoordinate2DMake(latitude, longitude)
        let region = CLCircularRegion(center: center, radius: radius, identifier: regionId)
        locationManager.startMonitoring(for: region)
        
        // save region id
        
        if regionIds == nil {
            regionIds = [String]()
        }
        regionIds?.append(regionId)
        UserDefaults.standard.set(regionIds?.joined(separator: REGION_SEPARATOR), forKey: REGIONS)
        UserDefaults.standard.synchronize()
    }
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        print("Entering region \(region.identifier)")
        
        if let regionIds = getRegionIds() {
            if regionIds.contains(region.identifier) {
                notificationManager.enableAlarm(regionId: region.identifier)
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        print("Exiting region \(region.identifier)")
        
        if let regionIds = getRegionIds() {
            if regionIds.contains(region.identifier) == true {
                notificationManager.disableAlarm(regionId: region.identifier)
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let center = CLLocation(latitude: 0, longitude: 0)
        let distance = locations[0].distance(from: center)
        print(distance)
    }
    
    func checkRequirements() -> (Bool, [String], [String]) {
        var errors = [String]()
        var warnings = [String]()
        
        if (!CLLocationManager.isMonitoringAvailable(for: CLRegion.self)) {
            errors.append("Geofencing not available")
        }
        
        if (!CLLocationManager.locationServicesEnabled()) {
            errors.append("Error: Locationservices not enabled")
        }
        
        let authStatus = CLLocationManager.authorizationStatus()
        
        if (authStatus != CLAuthorizationStatus.authorizedAlways) {
            errors.append("Warning: Location always permissions not granted")
        }
        
        if (iOS8) {
            if let notificationSettings = UIApplication.shared.currentUserNotificationSettings {
                if notificationSettings.types == UIUserNotificationType() {
                    errors.append("Error: notification permission missing")
                } else {
                    if !notificationSettings.types.contains(.sound) {
                        warnings.append("Warning: notification settings - sound permission missing")
                    }
                    
                    if !notificationSettings.types.contains(.alert) {
                        warnings.append("Warning: notification settings - alert permission missing")
                    }
                    
                    if !notificationSettings.types.contains(.badge) {
                        warnings.append("Warning: notification settings - badge permission missing")
                    }
                }
            } else {
                errors.append("Error: notification permission missing")
            }
        }
        
        let ok = (errors.count == 0)
        
        return (ok, warnings, errors)
    }
    
}


@available(iOS 10.0, *)
class NotificationManager : NSObject {
    let notificationCenter = UNUserNotificationCenter.current()
    
    override init() {
        print("Notification init")
        super.init()
        
        notificationCenter.requestAuthorization(options: [.alert, .sound, .badge]) { (success, error) in
            if let error = error {
                print("Request Authorization Failed (\(error), \(error.localizedDescription))")
            }
        }
    }
    
    func getTimes(_ regionId: String) -> [String]? {
        let str = UserDefaults.standard.string(forKey: "times_\(regionId)")
        return str?.components(separatedBy: TIME_SEPARATOR)
    }
    
    func addAlarm(regionId: String, time: String, title: String, text: String) {
        let alarmId = "\(regionId)_\(time)"
        
        removeAlarm(regionId: regionId, time: time)
        
        var times = getTimes(regionId)
        if times == nil {
            times = [String]()
        }
        times?.append(time)
        
        UserDefaults.standard.set(times?.joined(separator: TIME_SEPARATOR), forKey: "times_\(regionId)")
        UserDefaults.standard.set(title, forKey: "\(alarmId)_title")
        UserDefaults.standard.set(text, forKey: "\(alarmId)_text")
        UserDefaults.standard.synchronize()
    }
    
    func removeAlarm(regionId: String) {
        disableAlarm(regionId: regionId)
        UserDefaults.standard.removeObject(forKey: "times_\(regionId)")
        UserDefaults.standard.synchronize()
    }
    
    func removeAlarm(regionId: String, time: String) {
        if var times = getTimes(regionId) {
            if let index = times.index(of: time) {
                notificationCenter.removePendingNotificationRequests(withIdentifiers: ["\(regionId)_\(time)"])

                times.remove(at: index)
                if times.count > 0 {
                    UserDefaults.standard.set(times.joined(separator: TIME_SEPARATOR), forKey: "times_\(regionId)")
                } else {
                    UserDefaults.standard.removeObject(forKey: "times_\(regionId)")
                }
                UserDefaults.standard.synchronize()
            }
        }
    }
    
    func enableAlarm(regionId: String) {
        if let times = getTimes(regionId) {
            for time in times {
                enableAlarm(regionId: regionId, time: time)
            }
        }
    }
    
    func enableAlarm(regionId: String, time: String) {
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        let date = fmt.date(from: time)
        var dateInfo = DateComponents()
        dateInfo.hour = Calendar.current.component(.hour, from: date!)
        dateInfo.minute = Calendar.current.component(.minute, from: date!)
        
        let alarmId = "\(regionId)_\(time)"
        let content = UNMutableNotificationContent()
        content.sound = UNNotificationSound.default()
        content.title = UserDefaults.standard.string(forKey: "\(alarmId)_title")!
        content.body = UserDefaults.standard.string(forKey: "\(alarmId)_text")!
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateInfo, repeats: true)
        let request = UNNotificationRequest(identifier: alarmId, content: content, trigger: trigger)
        notificationCenter.add(request)
    }
    
    func disableAlarm(regionId: String) {
        if let times = getTimes(regionId) {
            var ids = [String]()
            for time in times {
                ids.append("\(regionId)_\(time)")
            }
            notificationCenter.removePendingNotificationRequests(withIdentifiers: ids)
        }
    }
    
}
