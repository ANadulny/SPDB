import React from "react";
import L, { marker } from 'leaflet';
import "./App.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { Button, Table } from 'react-bootstrap';
import Collapsible from 'react-collapsible';
var osmtogeojson = require('osmtogeojson')

require('leaflet-routing-machine'); // Adds L.Routing onto L

class App extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      startingPoint: { lat: 0.0, lng: 0.0 },
      activeMapFeature: null,
      startingPointMarker: new L.Marker([0,0]),
      startingPointRadius: new L.circle([0,0],0),
      geoJsonLayer: null,
      displaySearchPanel: false,
      
      time: '',
      length: '',
      routeLength: null,
      searchedObject: new TagList(),
      searchedFeatures: [new TagList()],
      isAnd: true,
      radius: 0,
      vehicle: "",
      time: 0,
      precision: 100.0
    };
    this.handleChange = this.handleChange.bind(this);
    this.searchPlace = this.searchPlace.bind(this);
    this.callback = this.callback.bind(this);
    this.handleStartingPointChange = this.handleStartingPointChange.bind(this);
    this.handleFeatureTagSelect = this.handleFeatureTagSelect.bind(this);
    this.removeSearchedFeature = this.removeSearchedFeature.bind(this);
    this.addNewRowForSearchedFeature = this.addNewRowForSearchedFeature.bind(this);
    this.handleDistanceForSearchedFeaturesChange = this.handleDistanceForSearchedFeaturesChange.bind(this);
    this.handleSearchedObjectTagSelect = this.handleSearchedObjectTagSelect.bind(this);
  }

  onEachFeature(feature, layer) {
    // does this feature have a property named popupContent?
    if (feature.properties && feature.properties.popupContent) {
        layer.bindPopup(feature.properties.popupContent);
    }else if(feature.properties && feature.properties.tags.name){
      	layer.bindPopup(feature.properties.tags.name);
    }
  }

  callback(data){
    if(false){
      alert("error: ");
    }
    else{
      var options = options || {};
      var geojson = osmtogeojson(data, {
        flatProperties: options.flatProperties || false
      });

      var geoJsonLayer = L.geoJSON(geojson, {onEachFeature: this.onEachFeature});
      
      this.state.geoJsonLayer.clearLayers();
      geoJsonLayer.addTo(this.state.map);
      this.setState({
        geoJsonLayer: geoJsonLayer
      });
    }
  }

  createTagList(elemList){
    var tagsToSend = [];
    for(var i = 0; i<elemList.length-1; i++){
      if(elemList[i+1] !== null)
      tagsToSend.push({
        category: elemList[i],
        objectType: elemList[i+1]
      })
    }
    return tagsToSend;
  }

  searchPlace(event){
    //Do stuff
    alert("searching stuff!");
    var tagsToSend = this.createTagList(this.state.searchedObject.elemList);

    var searchedFeaturesToSend = [];
    for(var i = 0; i<this.state.searchedFeatures.length; i++){
      var tagsForFeature = this.createTagList(this.state.searchedFeatures[i].elemList);
      searchedFeaturesToSend.push({
        tags: tagsForFeature,
        distance: this.state.searchedFeatures[i].distance,
        time: this.state.time
      });
    }
    

    var dataToSend = {
      startingPoint: this.state.startingPoint,
      searchedObject: {
        tags: tagsToSend,
        distance: this.state.radius,
        time: this.state.time
      },
      searchedObjects: searchedFeaturesToSend,
      isAnd: this.state.isAnd,
      distance: this.state.radius,
      time: this.state.time,
      precision: this.state.precision,
      vehicleType: this.state.vehicle
    }
    console.log(JSON.stringify(dataToSend));
    const url = "/api";
    const jsonString = JSON.stringify(dataToSend);
    var headers = {
      "Accept": "application/json",
      "Content-Type": "application/json"
    };  

    fetch(url,{
      method: "POST",
      headers: headers,
      body: jsonString
    })
    .then(res=>res.json())
    .then(json => {
      console.log(json);
      this.callback(json);
    }).catch(err => {
      alert("There was some kind of error with response!");
    });
  }

  componentDidMount(){
    var map = L.map('map').setView([52.2366, 21.0030], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			maxZoom: 18,
			attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
			id: 'mapbox/light-v9'
    }).addTo(map);

    function createButton(label, container) {
      var btn = L.DomUtil.create('button', '', container);
      btn.setAttribute('type', 'button');
      btn.innerHTML = label;
      return btn;
    }

    let markerGroup = L.layerGroup().addTo(map);
    this.state.startingPointMarker.addTo(markerGroup);
    this.state.startingPointRadius.addTo(map);

    map.on('click', function(e) {
        var container = L.DomUtil.create('div'),
            startBtn = createButton('Start from this location', container);
        startBtn.onclick = function () { 
           this.state.startingPointMarker.setLatLng([e.latlng.lat, e.latlng.lng]);
           this.state.startingPointRadius.setLatLng([e.latlng.lat, e.latlng.lng]);
           this.setState({
            startingPoint: { lat: e.latlng.lat, lng: e.latlng.lng }
           });
           map.closePopup();
         }.bind(this);
        L.popup()
            .setContent(container)
            .setLatLng(e.latlng)
            .openOn(map);
    }.bind(this));

    var geoJsonLayer = L.geoJSON();
    geoJsonLayer.addTo(map)

    this.setState({
      map: map,
      geoJsonLayer: geoJsonLayer
    })
  }

  async validateForm(event) {
    console.log("validateForm");
    // var x, text;
    // x = document.forms["userSearchingInputData"]["lat"].value;
    // if (isNaN(x) || x < 1 || x > 10) {
    //   text = "Input not valid";
    // } else {
    //   text = "Input OK";
    // }
    // document.getElementById("demo").innerHTML = text;
    // document.getElementById("submit").disabled = false;
  }

  handleChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    if(target.type === "checkbox"){
      if(this.state.isAnd)
        this.setState({isAnd: false});
      else
        this.setState({isAnd: true});
      return;
    }
    if(name === "radius"){
      this.state.searchedObject.distance = Number(value);
      this.state.startingPointRadius.setRadius(Number(value));
    }
    this.setState({
      [name]: value
    });
  }

  handleStartingPointChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    if(this.state.startingPointMarker != null){
    if(name === 'lat'){
      this.setState({
        startingPoint: { lat: value, lng: this.state.startingPoint.lng }
      }, () => {
        if(this.state.startingPointMarker != null){
          this.state.startingPointMarker.setLatLng(this.state.startingPoint);
          this.state.startingPointRadius.setLatLng(this.state.startingPoint);
      }});
    }else{
      this.setState({
        startingPoint: { lat: this.state.startingPoint.lat, lng: value }
      }, () => {
        if(this.state.startingPointMarker != null){
          this.state.startingPointMarker.setLatLng(this.state.startingPoint);
      }});
    }
    }
  }

  handleFeatureTagSelect(event){
    const target = event.target;
    const value = target.value.split(",");
    const name = target.name;
    var featuresList = this.state.searchedFeatures[Number(value[0])];
    
    if(value[2] === "ignore"){
      for(var i = Number(value[1]); i < featuresList.elemList.length; i++){
        featuresList.elemList[i] = null;
      }
    }else{
      featuresList.elemList[value[1]] = value[2];
      for(var i = Number(value[1])+1; i<featuresList.elemList.length; i++){
        featuresList.elemList[i] = null;
      }
    }
    this.setState(state => {
      const list = state.searchedFeatures.map((item, j) => {
        if (Number(value[1]) === j) {
          return featuresList
        } else {
          return item;
        }
      });
      return {
        list,
      };
    }, () => this.render());
    //alert("you've selected: " + event.target.value);
  }

  removeSearchedFeature(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    console.log(value + name);
    var searchedFeatures = this.state.searchedFeatures;
    searchedFeatures.splice(Number(value), 1);
    this.setState({
      searchedFeatures: searchedFeatures
    }, () => this.render());
    //alert("remove: " + value);
    //TO DO!
  }

  addNewRowForSearchedFeature(event){
    var featuresList = this.state.searchedFeatures;
    featuresList.push(new TagList());
    this.setState({
      searchedFeatures: featuresList
    }, () => this.render())
  }

  renderAdditionalFields(rowIndex, columnIndex, key, fun){
    if(key === null)
      return <td></td>;
    var toReturn; 
    var availableFeatures = new AvailableFeatures();
    if(Array.isArray(key)){
      for(var i = 0; i < key.length; i++){
        if(key[i] === null)
          return <td></td>;
      }
      var returnNow = true;
      for(var i = 0; i< availableFeatures.features[key[0]].length; i++){
        if(availableFeatures.features[key[0]][i][key[1]] !== undefined && typeof(availableFeatures.features[key[0]][i]) !== "string")
          returnNow = false;
      }
      if(returnNow)
        return <td></td>
      toReturn = <td>
        {availableFeatures.features[key[0]] !== undefined &&
          <select onChange = {fun}>
          <option value={[rowIndex, columnIndex, "ignore"]}>---Please select type---</option>
          {availableFeatures.features[key[0]].map((elem) => {
            if(elem[key[1]] !== undefined && typeof(elem) !== "string"){
              return elem[key[1]].map((elem1) =>{
                return <option value={[rowIndex, columnIndex, elem1]}>{elem1}</option>;
            });
            }
          })}
        </select>
      }</td>;
    }else{
      toReturn = <td>
        <select onChange = {fun}>
          <option value={[rowIndex, columnIndex, "ignore"]}>---Please select type---</option>
          {availableFeatures.features[key].map((elem) => {
            if(typeof(elem) === "string"){
              return <option value={[rowIndex, columnIndex, elem]}>{elem}</option>;
            }else{
              return Object.entries(elem).map(([key, value]) => {
                return <option value={[rowIndex, columnIndex, key]}>{key}</option>
              });
            }
          })}
        </select>
      </td>;
    }
    return toReturn;
  }

  handleDistanceForSearchedFeaturesChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;

    var featuresList = this.state.searchedFeatures[Number(name)];
    featuresList.distance = Number(value);

    this.setState(state => {
      const list = state.searchedFeatures.map((item, j) => {
        if (Number(name) === j) {
          return featuresList
        } else {
          return item;
        }
      });
      return {
        list,
      };
    }, () => this.render());
  }

  handleSearchedObjectTagSelect(event){
    const target = event.target;
    const value = target.value.split(",");
    const name = target.name;

    var searchedObject = this.state.searchedObject;
    
    if(value[2] === "ignore"){
      for(var i = Number(value[1]); i < searchedObject.elemList.length; i++){
        searchedObject.elemList[i] = null;
      }
    }else{
      searchedObject.elemList[value[1]] = value[2];
      for(var i = Number(value[1])+1; i<searchedObject.elemList.length; i++){
        searchedObject.elemList[i] = null;
      }
    }
    this.setState({
      searchedObject: searchedObject
    }, () => this.render());

  }

  render(){
    var availableFeatures = new AvailableFeatures();
    var rowIndex = -1;
    var searchedFeatures;
    
    var isAndBox;
    if(this.state.isAnd){
      isAndBox = <input type="checkbox" name="isAnd" value={this.state.isAnd} onChange={this.handleChange} checked></input>;
    }
    else{
      isAndBox = <input type="checkbox" name="isAnd" value={this.state.isAnd} onChange={this.handleChange}></input>;
    }

    searchedFeatures = <tbody>
      {this.state.searchedFeatures.map((elem) => {
        rowIndex++;
        return (
          <tr>
          <th scope="row">{rowIndex+1}</th>
          <td>
            <select onChange = {this.handleFeatureTagSelect}>
            <option value={[rowIndex, 0, "ignore"]}>---Please select type---</option>
            {Object.entries(availableFeatures.features).map(([key, value]) => {
              return (<option value={[rowIndex, 0, key]} class="boldOption">{key}</option>)
              }
            )}
            </select>
          </td>
            {this.renderAdditionalFields(rowIndex, 1, this.state.searchedFeatures[rowIndex].elemList[0], this.handleFeatureTagSelect)}
            {this.renderAdditionalFields(rowIndex, 2, [this.state.searchedFeatures[rowIndex].elemList[0], this.state.searchedFeatures[rowIndex].elemList[1]], this.handleFeatureTagSelect)}
          <td><input onChange = {this.handleDistanceForSearchedFeaturesChange} name={rowIndex} value={this.state.searchedFeatures[rowIndex].distance}></input></td>
          <td><Button onClick = {this.removeSearchedFeature} value={rowIndex} variant="danger">Remove</Button></td>
          </tr>)}
      )}
    </tbody>;

    var searchedObject = <div>
      <table>
        <tr>
          <td>
            <select onChange = {this.handleSearchedObjectTagSelect}>
            <option value={[rowIndex, 0, "ignore"]}>---Please select type---</option>
            {Object.entries(availableFeatures.features).map(([key, value]) => {
              return (<option value={[0, 0, key]} class="boldOption">{key}</option>)
              }
            )}
            </select>
          </td>
            {this.renderAdditionalFields(rowIndex, 1, this.state.searchedObject.elemList[0], this.handleSearchedObjectTagSelect)}
            {this.renderAdditionalFields(rowIndex, 2, [this.state.searchedObject.elemList[0], this.state.searchedObject.elemList[1]], this.handleSearchedObjectTagSelect)}
        </tr>
      </table>
    </div>

    return (
      <div class="border p-3">
        <Collapsible trigger="Search Panel">
        <div class="content">
          <div class="border p-3">
            <h2>Starting point</h2>
            <label>latitude: </label>
            <input type='textbox' name='lat' value={this.state.startingPoint.lat} onChange={this.handleStartingPointChange}></input>
            <label>longitude:</label>
            <input type='textbox' name='lng' value={this.state.startingPoint.lng} onChange={this.handleStartingPointChange}></input>
            <label>radius:</label>
            <input type='textbox' name='radius' value={this.state.radius} onChange={this.handleChange}></input>
            <label>precision:</label>
            <input name="precision" value={this.state.precision} onChange={this.handleChange}></input>
          </div>
          <div class="border p-3">
            <table align="center">
            {searchedObject}
            </table>
          </div>
          <div class="border p-3">
            <table class="table table-striped table-bordered table-hover searchedFeatures">
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Feature</th>
                  <th scope="col">SubFeature</th>
                  <th scope="col">SubFeature</th>
                  <th scope="col">Distance</th>
                  <th scope="col">Action</th>
                </tr>
              </thead>
              {searchedFeatures}
            </table>
            <Button onClick = {this.addNewRowForSearchedFeature} variant="primary">Add</Button>
            <br></br>
            <label>Conjunction?</label>
            {isAndBox}
            <small class="form-text text-muted">Do you need all additional features?</small>
          </div>
          <div class="border p-3">
            <label>Vehicle: </label>
            <select name="vehicle" onChange={this.handleChange}>
              <option value="">-Select Vehicle-</option>
              <option>Car</option>
              <option>Bike</option>
              <option>Foot</option>
            </select>
            <label>Time (s)</label>
            <input onChange={this.handleChange} value={this.state.time} name="time"></input>
            <br></br>
            <Button onClick={this.searchPlace} variant="success">Submit</Button>
          </div>
          <br />
        </div>
        </Collapsible>
      <div className = "border p-3">
        <div id="map"></div>
      </div>
    </div>
    );
  }
}

class AvailableFeatures{
  features = {
    "amenity": ["bicycle_parking", "bicycle_rental", "bus_station", "car_wash"],
    "natural": [{
      "water": ["lake", "rivier", "pond"],
      "hill": ["big", "medium", "small"]
    }, "beach", "wood"],
    "tourism": ["hotel", "guest_house", "camp_site"]
  }
}
class TagList{
  elemList = [null, null, null];
  distance = 0;
  //0 - cat
  //1 - subcat/tag
  //2 - tag for subcat
}

export default App;
