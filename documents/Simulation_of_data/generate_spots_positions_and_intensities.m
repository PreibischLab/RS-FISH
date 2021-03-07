function dlocpix = generate_spots_positions_and_intensities(stack,npts,limits,brightness,vox_size,psf)
%% input
%stack: the base 2d or 3d-image that specifies the size of the image on
%which the spots are to be generated
%number of points npts
%limits: array [xmin ymin zmin;xmax ymax zmax] that specifies the range in
%pix of the region where the the spots can be (further restricted by subtratcting 3 psf radii in each limit)
%brightness: [Itot, sdItot] the mean and sd of the integrated intensity of
    %each spot
%vox_size = [dx dy dz] in nm
%psf = [sigma_xy sigma_z] in nm

%% output
%dlocpix = [x y z I] is a npts by 4 array with the positions of the center
%of each spot and its integrated intensity


if ndims(stack) == 3
%% 3d    
    dlocpix = zeros(npts,4);
    dlocpix(1:npts,4) = randn(npts,1)*brightness(2) + brightness(1);
    dlocpix(:,4) = abs(dlocpix(:,4)); %enforcing posiive spot intensitiy values
    if limits == 0
        [nx ny nz] = size(stack);
        limits = [0,0,0;nx,ny,nz];    
    end

    xc = (limits(1,1)+limits(2,1))/2.0;    %x center of the ROI or image (in nm)
    yc = (limits(1,2)+limits(2,2))/2.0;    %y center of the ROI or image (in nm)
    zc = (limits(1,3)+limits(2,3))/2.0;    %x center of the ROI or image (in nm)

    xrange = (limits(2,1)-limits(1,1))/2.0 - 3.0*psf(1)/vox_size(1);    %max x distance of a spot from the center of the ROI or image (in nm)
    yrange = (limits(2,2)-limits(1,2))/2.0 - 3.0*psf(1)/vox_size(2);    %max y distance of a spot from the center of the ROI or image (in nm)
    zrange = (limits(2,3)-limits(1,3))/2.0 - 3.0*psf(2)/vox_size(3);    %max z distance of a spot from the center of the ROI or image (in nm)

    dlocpix(1:npts,1:3) = 2.0*(rand(npts,3)-0.5);
    dlocpix(1:npts,1) = dlocpix(1:npts,1)*xrange + xc;
    dlocpix(1:npts,2) = dlocpix(1:npts,2)*yrange + yc;
    dlocpix(1:npts,3) = dlocpix(1:npts,3)*zrange + zc;


elseif ndims(stack) == 2
%% 2d    
    dlocpix = zeros(npts,3);
    dlocpix(1:npts,3) = randn(npts,1)*brightness(2) + brightness(1);
    dlocpix(:,3) = abs(dlocpix(:,3)); %enforcing posiive spot intensitiy values
    if limits == 0
        [nx ny] = size(stack);
        limits = [0,0;nx,ny];    
    end

    xc = (limits(1,1)+limits(2,1))/2.0;    %x center of the ROI or image (in nm)
    yc = (limits(1,2)+limits(2,2))/2.0;    %y center of the ROI or image (in nm)
    
    xrange = (limits(2,1)-limits(1,1))/2.0 - 3.0*psf(1)/vox_size(1);    %max x distance of a spot from the center of the ROI or image (in nm)
    yrange = (limits(2,2)-limits(1,2))/2.0 - 3.0*psf(1)/vox_size(2);    %max y distance of a spot from the center of the ROI or image (in nm)
    
    dlocpix(1:npts,1:2) = 2.0*(rand(npts,2)-0.5);
    dlocpix(1:npts,1) = dlocpix(1:npts,1)*xrange + xc;
    dlocpix(1:npts,2) = dlocpix(1:npts,2)*yrange + yc;
    
end
    
end