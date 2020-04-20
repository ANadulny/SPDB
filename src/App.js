import React from "react";
import { Map, Marker, Popup, TileLayer } from "react-leaflet";
import { Icon } from "leaflet";
import * as parkData from "./data/skateboard-parks.json";
import "./App.css";

export const icon = new Icon({
  iconUrl: "/skateboarding.svg",
  iconSize: [25, 25]
});

class App extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      activePark: null,
      searchedPlace: ""
    };
    this.handleChange = this.handleChange.bind(this);
    this.searchPlace = this.searchPlace.bind(this);
  }

  handleChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    this.setState({
      [name]: value
    });
  }

  searchPlace(event){
    //TO DO
    alert("You are looking for: " + this.state.searchedPlace);
  }

  render(){
    return (
      <div>
        <div>
        <input type="textbox" onChange={this.handleChange} name="searchedPlace"></input>
        <button onClick = {this.searchPlace}>Search!</button>
        </div>
        <label>Searched Place: </label>
        <label>{this.state.searchedPlace}</label>
        <Map center={[45.4, -75.7]} zoom={12}>
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
          />
  
          {parkData.features.map(park => (
            <Marker
              key={park.properties.PARK_ID}
              position={[
                park.geometry.coordinates[1],
                park.geometry.coordinates[0]
              ]}
              onClick={() => {
                this.setState({ activePark: park});
                //setActivePark(park);
              }}
              icon={icon}
            />
          ))}
  
          {this.state.activePark && (
            <Popup
              position={[
                this.state.activePark.geometry.coordinates[1],
                this.state.activePark.geometry.coordinates[0]
              ]}
              onClose={() => {
                this.setState({ activePark: null});
                //setActivePark(null);
              }}
            >
              <div>
                <h2>{this.state.activePark.properties.NAME}</h2>
                <p>{this.state.activePark.properties.DESCRIPTIO}</p>
              </div>
            </Popup>
          )}
        </Map>
      </div>
    );
  }
}

export default App;

/*export default function App() {

  const [activePark, setActivePark] = React.useState(null);

  return (
    <div>
      <Map center={[45.4, -75.7]} zoom={12}>
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        />

        {parkData.features.map(park => (
          <Marker
            key={park.properties.PARK_ID}
            position={[
              park.geometry.coordinates[1],
              park.geometry.coordinates[0]
            ]}
            onClick={() => {
              setActivePark(park);
            }}
            icon={icon}
          />
        ))}

        {activePark && (
          <Popup
            position={[
              activePark.geometry.coordinates[1],
              activePark.geometry.coordinates[0]
            ]}
            onClose={() => {
              setActivePark(null);
            }}
          >
            <div>
              <h2>{activePark.properties.NAME}</h2>
              <p>{activePark.properties.DESCRIPTIO}</p>
            </div>
          </Popup>
        )}
      </Map>
    </div>
  );
}
*/
