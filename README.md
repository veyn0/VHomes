This Project is a Rework of my [Homesystem](https://github.com/veyn0/homesystem) Minecraft Plugin.
It aims to significantly improve stability while also creating a better structured base in order to add more features in the future


| Version           | Tested Versions | Folia | Paper | Warps | Name Changeable|
|--------------------|--------|--------|--------|--------|--------|
| [1.21.11](https://github.com/veyn0/VHomes/releases/latest)      | 1.21.4 - 1.21.11 |✅     |✅     |❌     |❌     | 


## Features (Completed / Planned)

- [x] GUI
- [X] Most of the Labels are configurable
- [x] Folia Support
- [ ] Warp System / public Homes
- [ ] Edit Names
- [ ] Inventory Navigation labels and Items Configurable
- [ ] Create Home Through Inventory
- [ ] PAPI Integration
- [ ] Admin features to view / delete homes of players


## Commands

| Name | Aliases | Permission | description |  
| ---- | ---- | --- | --- |
| home | homes, delhome | vhomes.use | Opens the home overview GUI |
| sethome |  | vhomes.set | creates a home at the location of the Player |


## Permissions

| Name | default | description |
| --- | --- | --- |
| vhomes.use | true | allowes the player to teleport, delete and rename his homes |
| vhomes.set | true | allowes the player to create homes |
| vhomes.max.x | 5 | sets the maximum allowed homes for a player up to 100 |
